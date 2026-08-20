package com.team2.wellness.infrastructure.openai;

import com.team2.wellness.engagement.ai.application.ImageGenerationException;
import com.team2.wellness.engagement.port.out.FaceReferenceValidationPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
public class OpenAiFaceReferenceValidationAdapter implements FaceReferenceValidationPort {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final String VALIDATION_PROMPT = """
            Act as a strict image-quality gate, not as an identity matcher. Reply with exactly VALID or INVALID.
            Reply VALID only when the supplied image is a photographic image containing exactly one clearly visible
            human face with enough facial detail to preserve that same person's identity in an image edit. The face must
            be reasonably large, in focus, and show recognizable eyes, nose, mouth, face shape, skin tone, and hairstyle.
            Reply INVALID for silhouettes, placeholders, icons, drawings, heavily filtered or obscured faces, tiny or
            blurry faces, images without a face, or images containing multiple people. Never infer missing facial details.
            """;

    private final WebClient client;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public OpenAiFaceReferenceValidationAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${app.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.face-validation-model:gpt-4o-mini}") String model,
            @Value("${app.openai.timeout-seconds:90}") long timeoutSeconds
    ) {
        this.client = webClientBuilder
                .baseUrl(stripTrailingSlash(baseUrl))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public boolean isUsableIdentityReference(byte[] image, String contentType) {
        if (image == null || image.length == 0 || contentType == null) return false;
        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalizedType)) return false;
        if (apiKey == null || apiKey.isBlank()) {
            throw new ImageGenerationException("OPENAI_API_KEY_MISSING", false);
        }

        String cacheKey = digest(normalizedType, image);
        Boolean cached = cache.get(cacheKey);
        if (cached != null) return cached;
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();

        boolean result = validate(image, normalizedType);
        cache.put(cacheKey, result);
        return result;
    }

    private boolean validate(byte[] image, String contentType) {
        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);
        Map<String, Object> body = Map.of(
                "model", model,
                "store", false,
                "max_output_tokens", 16,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", VALIDATION_PROMPT),
                                Map.of("type", "input_image", "image_url", dataUrl, "detail", "high")
                        )
                ))
        );

        try {
            OpenAiResponse response = client.post()
                    .uri("/v1/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, this::classifyRateLimit)
                    .onStatus(status -> status.is5xxServerError(), ignored -> retryable("OPENAI_SERVER_ERROR"))
                    .onStatus(status -> status.is4xxClientError(), ignored -> nonRetryable("OPENAI_REFERENCE_CHECK_REJECTED"))
                    .bodyToMono(OpenAiResponse.class)
                    .block(timeout);
            String verdict = extractVerdict(response);
            if ("VALID".equals(verdict)) return true;
            if ("INVALID".equals(verdict)) return false;
            throw new ImageGenerationException("OPENAI_REFERENCE_CHECK_INVALID_RESPONSE", false);
        } catch (ImageGenerationException exception) {
            throw exception;
        } catch (WebClientRequestException exception) {
            throw new ImageGenerationException("OPENAI_NETWORK_ERROR");
        }
    }

    private String extractVerdict(OpenAiResponse response) {
        if (response == null || response.output() == null) return null;
        for (OutputItem item : response.output()) {
            if (item == null || item.content() == null) continue;
            for (OutputContent content : item.content()) {
                if (content != null && "output_text".equals(content.type()) && content.text() != null) {
                    return content.text().trim().toUpperCase(Locale.ROOT);
                }
            }
        }
        return null;
    }

    private Mono<? extends Throwable> retryable(String code) {
        return Mono.just(new ImageGenerationException(code, true));
    }

    private Mono<? extends Throwable> nonRetryable(String code) {
        return Mono.just(new ImageGenerationException(code, false));
    }

    private Mono<? extends Throwable> classifyRateLimit(ClientResponse response) {
        return response.bodyToMono(OpenAiErrorResponse.class)
                .defaultIfEmpty(new OpenAiErrorResponse(null))
                .map(body -> {
                    OpenAiError error = body.error();
                    String message = error == null ? null : error.message();
                    String code = error == null ? null : error.code();
                    if (requiresBilling(code, message)) {
                        return new ImageGenerationException("OPENAI_BILLING_REQUIRED", false);
                    }
                    return new ImageGenerationException("OPENAI_RATE_LIMIT", true);
                });
    }

    private boolean requiresBilling(String code, String message) {
        if (code != null && Set.of(
                "credit_balance_exhausted",
                "organization_spend_limit_exceeded",
                "project_spend_limit_exceeded",
                "organization_usage_limit_exceeded",
                "insufficient_quota"
        ).contains(code)) return true;
        if (message == null) return false;
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("limit 0")
                || normalized.contains("payment method")
                || normalized.contains("billing");
    }

    private String digest(String contentType, byte[] image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(contentType.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(image));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record OpenAiResponse(OutputItem[] output) {
    }

    private record OutputItem(OutputContent[] content) {
    }

    private record OutputContent(String type, String text) {
    }

    private record OpenAiErrorResponse(OpenAiError error) {
    }

    private record OpenAiError(String message, String type, String code) {
    }
}
