package com.team2.wellness.core.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "avatar_object_path", length = 500)
    private String avatarObjectPath;

    @Column(name = "ai_face_consent", nullable = false)
    private boolean aiFaceConsent;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Profile() {
    }

    public Profile(UUID id, String nickname, String timezone) {
        Instant now = Instant.now();
        this.id = id;
        this.nickname = nickname;
        this.timezone = timezone;
        this.aiFaceConsent = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String nickname, String timezone, String avatarObjectPath, boolean aiFaceConsent) {
        this.nickname = nickname;
        this.timezone = timezone;
        this.avatarObjectPath = avatarObjectPath;
        this.aiFaceConsent = aiFaceConsent;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarObjectPath() {
        return avatarObjectPath;
    }

    public boolean isAiFaceConsent() {
        return aiFaceConsent;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
