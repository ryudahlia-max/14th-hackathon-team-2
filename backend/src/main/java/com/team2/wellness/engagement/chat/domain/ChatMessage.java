package com.team2.wellness.engagement.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "chat_messages", uniqueConstraints = @UniqueConstraint(name = "uk_chat_messages_client_id", columnNames = {"room_id", "sender_id", "client_message_id"}))
public class ChatMessage {
    public enum Type { TEXT, IMAGE, ROUTINE_CARD, AI_IMAGE, SYSTEM }
    @Id private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "sender_id") private UUID senderId;
    @Column(name = "client_message_id", length = 100) private String clientMessageId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Type type;
    @Column(columnDefinition = "text") private String content;
    @Column(name = "media_url", columnDefinition = "text") private String mediaUrl;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected ChatMessage() { }
    public ChatMessage(UUID roomId, UUID senderId, String clientMessageId, Type type, String content, String mediaUrl) {
        this.id = UUID.randomUUID(); this.roomId = roomId; this.senderId = senderId; this.clientMessageId = clientMessageId; this.type = type; this.content = content; this.mediaUrl = mediaUrl; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; } public UUID getRoomId() { return roomId; } public UUID getSenderId() { return senderId; } public String getClientMessageId() { return clientMessageId; } public Type getType() { return type; } public String getContent() { return content; } public String getMediaUrl() { return mediaUrl; } public Instant getCreatedAt() { return createdAt; }
}
