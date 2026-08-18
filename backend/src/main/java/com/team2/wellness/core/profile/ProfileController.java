package com.team2.wellness.core.profile;

import com.team2.wellness.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final AvatarStoragePort avatarStorage;

    public ProfileController(
            CurrentUser currentUser,
            ProfileService profileService,
            AvatarStoragePort avatarStorage
    ) {
        this.currentUser = currentUser;
        this.profileService = profileService;
        this.avatarStorage = avatarStorage;
    }

    @GetMapping
    ProfileResponse get(Authentication authentication) {
        return response(profileService.get(currentUser.id(authentication)));
    }

    @PostMapping("/bootstrap")
    ProfileResponse bootstrap(
            Authentication authentication,
            @Valid @RequestBody BootstrapRequest request
    ) {
        Profile profile = profileService.bootstrap(
                currentUser.id(authentication),
                request.nickname(),
                request.timezone(),
                request.termsVersion(),
                request.privacyVersion()
        );
        return response(profile);
    }

    @PatchMapping
    ProfileResponse update(Authentication authentication, @Valid @RequestBody UpdateRequest request) {
        Profile profile = profileService.update(
                currentUser.id(authentication),
                request.nickname(),
                request.timezone(),
                request.avatarObjectPath(),
                request.aiFaceConsent()
        );
        return response(profile);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ProfileResponse uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) throws java.io.IOException {
        UUID userId = currentUser.id(authentication);
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(java.util.Locale.ROOT))) {
            throw new com.team2.wellness.common.api.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_AVATAR_TYPE",
                    "이미지 파일만 업로드할 수 있습니다."
            );
        }
        AvatarStoragePort.StoredAvatar stored = avatarStorage.storeAvatar(
                userId,
                file.getBytes(),
                contentType
        );
        return response(profileService.updateAvatar(userId, stored.objectKey()));
    }

    private ProfileResponse response(Profile profile) {
        String avatarUrl = null;
        if (profile.getAvatarObjectPath() != null) {
            try {
                avatarUrl = avatarStorage.temporaryDownloadUrl(profile.getAvatarObjectPath());
            } catch (RuntimeException ignored) {
                avatarUrl = null;
            }
        }
        return ProfileResponse.from(profile, avatarUrl);
    }

    record BootstrapRequest(
            @NotBlank @Size(max = 30) String nickname,
            @NotBlank String timezone,
            @NotBlank String termsVersion,
            @NotBlank String privacyVersion
    ) {
    }

    record UpdateRequest(
            @NotBlank @Size(max = 30) String nickname,
            @NotBlank String timezone,
            @Size(max = 500) String avatarObjectPath,
            boolean aiFaceConsent
    ) {
    }

    record ProfileResponse(
            UUID id,
            String nickname,
            String avatarObjectPath,
            String avatarUrl,
            boolean aiFaceConsent,
            String timezone,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ProfileResponse from(Profile profile, String avatarUrl) {
            return new ProfileResponse(
                    profile.getId(),
                    profile.getNickname(),
                    profile.getAvatarObjectPath(),
                    avatarUrl,
                    profile.isAiFaceConsent(),
                    profile.getTimezone(),
                    profile.getCreatedAt(),
                    profile.getUpdatedAt()
            );
        }
    }
}
