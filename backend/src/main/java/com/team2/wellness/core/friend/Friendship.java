package com.team2.wellness.core.friend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "friendships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"first_user_id", "second_user_id"})
)
public class Friendship {

    public enum Status {
        ACCEPTED, BLOCKED, REMOVED
    }

    @Id
    private UUID id;

    @Column(name = "first_user_id", nullable = false)
    private UUID firstUserId;

    @Column(name = "second_user_id", nullable = false)
    private UUID secondUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Friendship() {
    }

    public Friendship(UUID userId, UUID otherUserId) {
        UUID first = normalizeFirst(userId, otherUserId);
        UUID second = first.equals(userId) ? otherUserId : userId;
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.firstUserId = first;
        this.secondUserId = second;
        this.status = Status.ACCEPTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void remove() {
        this.status = Status.REMOVED;
        this.updatedAt = Instant.now();
    }

    public void block() {
        this.status = Status.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void accept() {
        this.status = Status.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public UUID otherUser(UUID userId) {
        if (firstUserId.equals(userId)) {
            return secondUserId;
        }
        if (secondUserId.equals(userId)) {
            return firstUserId;
        }
        throw new IllegalArgumentException("User is not part of friendship");
    }

    public static UUID normalizeFirst(UUID first, UUID second) {
        return first.toString().compareTo(second.toString()) < 0 ? first : second;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFirstUserId() {
        return firstUserId;
    }

    public UUID getSecondUserId() {
        return secondUserId;
    }

    public Status getStatus() {
        return status;
    }
}
