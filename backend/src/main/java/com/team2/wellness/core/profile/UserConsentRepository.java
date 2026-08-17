package com.team2.wellness.core.profile;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    boolean existsByUserIdAndTermsVersionAndPrivacyVersion(
            UUID userId,
            String termsVersion,
            String privacyVersion
    );
}
