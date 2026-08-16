package com.team2.wellness.core.profile;

import com.team2.wellness.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private final CurrentUser currentUser;
    private final ProfileService profileService;

    public ProfileController(CurrentUser currentUser, ProfileService profileService) {
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    @GetMapping
    ProfileResponse get(Authentication authentication) {
        return ProfileResponse.from(profileService.get(currentUser.id(authentication)));
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
        return ProfileResponse.from(profile);
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
        return ProfileResponse.from(profile);
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
            boolean aiFaceConsent,
            String timezone,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ProfileResponse from(Profile profile) {
            return new ProfileResponse(
                    profile.getId(),
                    profile.getNickname(),
                    profile.getAvatarObjectPath(),
                    profile.isAiFaceConsent(),
                    profile.getTimezone(),
                    profile.getCreatedAt(),
                    profile.getUpdatedAt()
            );
        }
    }
}
