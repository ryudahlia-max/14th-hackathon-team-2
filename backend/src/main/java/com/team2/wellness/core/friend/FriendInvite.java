package com.team2.wellness.core.friend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_invites")
public class FriendInvite {

    public enum Status {
        PENDING, ACCEPTED, REVOKED, EXPIRED
    }

    @Id
    private UUID id;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FriendInvite() {
    }

    public FriendInvite(UUID inviterId, String token, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.inviterId = inviterId;
        this.token = token;
        this.status = Status.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public void accept(UUID userId, Instant now) {
        this.status = Status.ACCEPTED;
        this.acceptedBy = userId;
        this.acceptedAt = now;
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInviterId() {
        return inviterId;
    }

    public String getToken() {
        return token;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
