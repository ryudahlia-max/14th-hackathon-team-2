package com.team2.wellness.infrastructure.supabase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SupabaseMediaStorageAdapter implements MediaStoragePort {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final ProfileRepository profileRepository;
    private final WebClient client;
    private final String supabaseUrl;
    private final String secretKey;
    private final String avatarBucket;
    private final String aiBucket;
    private final String chatBucket;
    private final int signedUrlSeconds;
    private final int maxBytes;

    public SupabaseMediaStorageAdapter(
            ProfileRepository profileRepository,
            WebClient.Builder webClientBuilder,
            @Value("${app.supabase.url}") String supabaseUrl,
            @Value("${app.supabase.secret-key:}") String secretKey,
            @Value("${app.supabase.storage.avatar-bucket:avatars}") String avatarBucket,
            @Value("${app.supabase.storage.ai-bucket:ai-results}") String aiBucket,
            @Value("${app.supabase.storage.chat-bucket:chat-media}") String chatBucket,
            @Value("${app.supabase.storage.signed-url-seconds:300}") int signedUrlSeconds,
            @Value("${app.supabase.storage.max-image-bytes:10485760}") int maxBytes
    ) {
        this.profileRepository = profileRepository;
        this.supabaseUrl = stripTrailingSlash(supabaseUrl);
        this.client = webClientBuilder.baseUrl(this.supabaseUrl).build();
        this.secretKey = secretKey;
        this.avatarBucket = avatarBucket;
        this.aiBucket = aiBucket;
        this.chatBucket = chatBucket;
        this.signedUrlSeconds = signedUrlSeconds;
        this.maxBytes = maxBytes;
    }

    @Override
    public Optional<StoredMedia> findFaceAsset(UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> profile.getAvatarObjectPath())
                .filter(path -> path != null && !path.isBlank())
                .map(path -> {
                    ObjectLocation location = parse(path, avatarBucket);
                    if (!location.bucket().equals(avatarBucket)) {
                        throw new IllegalArgumentException("Face assets must be stored in the avatar bucket");
                    }
                    return new StoredMedia(location.externalKey(), contentTypeFor(location.path()));
                });
    }

    @Override
    public byte[] read(String objectKey) {
        ObjectLocation location = parse(objectKey, avatarBucket);
        byte[] bytes = client.get()
                .uri(uriBuilder -> uriBuilder.path("/storage/v1/object/authenticated")
                        .pathSegment(location.bucket())
                        .pathSegment(location.segments())
                        .build())
                .headers(headers -> SupabaseApiHeaders.authenticate(headers, secretKey))
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(15));
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Storage returned an empty object");
        }
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException("Stored image exceeds the configured size limit");
        }
        return bytes;
    }

    @Override
    public StoredMedia storeAiOutput(UUID ownerId, byte[] bytes, String contentType) {
        return store(aiBucket, "ai-generations/" + ownerId + "/" + UUID.randomUUID() + extension(contentType), bytes, contentType);
    }

    @Override
    public StoredMedia storeChatMedia(UUID ownerId, byte[] bytes, String contentType) {
        return store(chatBucket, ownerId + "/" + UUID.randomUUID() + extension(contentType), bytes, contentType);
    }

    @Override
    public String resolveChatMedia(String objectKey) {
        ObjectLocation location = parse(objectKey, chatBucket);
        if (!location.bucket().equals(chatBucket)) {
            throw new IllegalArgumentException("Chat media must be stored in the chat bucket");
        }
        return temporaryDownloadUrl(location.externalKey());
    }

    @Override
    public String temporaryDownloadUrl(String objectKey) {
        ObjectLocation location = parse(objectKey, avatarBucket);
        SignedUrlResponse response = client.post()
                .uri(uriBuilder -> uriBuilder.path("/storage/v1/object/sign")
                        .pathSegment(location.bucket())
                        .pathSegment(location.segments())
                        .build())
                .headers(headers -> SupabaseApiHeaders.authenticate(headers, secretKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SignedUrlRequest(signedUrlSeconds))
                .retrieve()
                .bodyToMono(SignedUrlResponse.class)
                .block(Duration.ofSeconds(10));
        if (response == null || response.signedUrl() == null || response.signedUrl().isBlank()) {
            throw new IllegalStateException("Storage did not return a signed URL");
        }
        return response.signedUrl().startsWith("http")
                ? response.signedUrl()
                : supabaseUrl + response.signedUrl();
    }

    private StoredMedia store(String bucket, String path, byte[] bytes, String contentType) {
        validateImage(bytes, contentType);
        ObjectLocation location = new ObjectLocation(bucket, path);
        client.post()
                .uri(uriBuilder -> uriBuilder.path("/storage/v1/object")
                        .pathSegment(location.bucket())
                        .pathSegment(location.segments())
                        .build())
                .headers(headers -> SupabaseApiHeaders.authenticate(headers, secretKey))
                .header("x-upsert", "false")
                .contentType(MediaType.parseMediaType(contentType))
                .bodyValue(bytes)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(20));
        return new StoredMedia(location.externalKey(), contentType);
    }

    private void validateImage(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw new IllegalArgumentException("Image size is invalid");
        }
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported image content type");
        }
    }

    private ObjectLocation parse(String objectKey, String defaultBucket) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key is required");
        }
        String normalized = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        String[] pieces = normalized.split("/", 2);
        ObjectLocation location = pieces.length == 2 && isKnownBucket(pieces[0])
                ? new ObjectLocation(pieces[0], pieces[1])
                : new ObjectLocation(defaultBucket, normalized);
        location.validate();
        return location;
    }

    private boolean isKnownBucket(String value) {
        return value.equals(avatarBucket) || value.equals(aiBucket) || value.equals(chatBucket);
    }

    private String contentTypeFor(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    private String extension(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record ObjectLocation(String bucket, String path) {
        String[] segments() {
            return path.split("/");
        }

        String externalKey() {
            return bucket + "/" + path;
        }

        void validate() {
            if (bucket.isBlank() || path.isBlank()
                    || Arrays.stream(segments()).anyMatch(segment -> segment.isBlank()
                    || segment.equals(".") || segment.equals(".."))) {
                throw new IllegalArgumentException("Invalid Storage object key");
            }
        }
    }

    private record SignedUrlRequest(int expiresIn) {
    }

    private record SignedUrlResponse(@JsonProperty("signedURL") String signedUrl) {
    }
}
