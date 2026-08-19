package com.team2.wellness.engagement.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "message_reactions", uniqueConstraints = @UniqueConstraint(name = "uk_message_reactions_message_user_type", columnNames = {"message_id", "user_id", "type"}))
public class MessageReaction {
    @Id private UUID id;
    @Column(name = "message_id", nullable = false) private UUID messageId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 30) private String type;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected MessageReaction() { }
    public MessageReaction(UUID messageId, UUID userId, String type) { this.id = UUID.randomUUID(); this.messageId = messageId; this.userId = userId; this.type = type; this.createdAt = Instant.now(); }
    public UUID getId() { return id; } public UUID getMessageId() { return messageId; } public UUID getUserId() { return userId; } public String getType() { return type; } public Instant getCreatedAt() { return createdAt; }
}
