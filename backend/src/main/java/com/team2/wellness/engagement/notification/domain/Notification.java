package com.team2.wellness.engagement.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "dedup_key", unique = true, length = 180)
    private String dedupKey;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID userId, String type, String content) {
        this(userId, type, content, null);
    }

    public Notification(UUID userId, String type, String content, String dedupKey) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.dedupKey = dedupKey;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
}
