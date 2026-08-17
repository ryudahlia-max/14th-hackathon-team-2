package com.team2.wellness.core.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
public class UserConsent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "terms_version", nullable = false, length = 30)
    private String termsVersion;

    @Column(name = "privacy_version", nullable = false, length = 30)
    private String privacyVersion;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserConsent() {
    }

    public UserConsent(UUID userId, String termsVersion, String privacyVersion, Instant agreedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.agreedAt = agreedAt;
        this.createdAt = Instant.now();
    }
}
