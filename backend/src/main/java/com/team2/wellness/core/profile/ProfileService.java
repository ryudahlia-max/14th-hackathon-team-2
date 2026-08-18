package com.team2.wellness.core.profile;

import com.team2.wellness.common.api.ApiException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserConsentRepository consentRepository;

    public ProfileService(ProfileRepository profileRepository, UserConsentRepository consentRepository) {
        this.profileRepository = profileRepository;
        this.consentRepository = consentRepository;
    }

    public Profile get(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PROFILE_NOT_FOUND",
                        "프로필 설정이 필요합니다."
                ));
    }

    public Profile bootstrap(
            UUID userId,
            String nickname,
            String timezone,
            String termsVersion,
            String privacyVersion
    ) {
        validateTimezone(timezone);
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId, nickname.trim(), timezone)));
        if (!consentRepository.existsByUserIdAndTermsVersionAndPrivacyVersion(
                userId,
                termsVersion,
                privacyVersion
        )) {
            consentRepository.save(new UserConsent(userId, termsVersion, privacyVersion, Instant.now()));
        }
        return profile;
    }

    public Profile update(
            UUID userId,
            String nickname,
            String timezone,
            String avatarObjectPath,
            boolean aiFaceConsent
    ) {
        validateTimezone(timezone);
        Profile profile = get(userId);
        profile.update(nickname.trim(), timezone, normalizeBlank(avatarObjectPath), aiFaceConsent);
        return profile;
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (ZoneRulesException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "지원하지 않는 시간대입니다.");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
