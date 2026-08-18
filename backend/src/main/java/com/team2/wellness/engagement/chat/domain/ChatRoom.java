package com.team2.wellness.engagement.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_rooms_direct_pair", columnNames = "direct_pair_key"),
        @UniqueConstraint(name = "uk_chat_rooms_group", columnNames = "group_id")
})
public class ChatRoom {
    public enum Type { DIRECT, GROUP }
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Type type;
    @Column(name = "direct_pair_key", length = 80) private String directPairKey;
    @Column(name = "group_id") private UUID groupId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ChatRoom() { }
    private ChatRoom(UUID id, Type type, String directPairKey, UUID groupId) {
        this.id = id; this.type = type; this.directPairKey = directPairKey; this.groupId = groupId; this.createdAt = Instant.now();
    }
    public static ChatRoom direct(UUID first, UUID second) { return new ChatRoom(UUID.randomUUID(), Type.DIRECT, pairKey(first, second), null); }
    public static ChatRoom group(UUID groupId) { return new ChatRoom(UUID.randomUUID(), Type.GROUP, null, groupId); }
    public static String pairKey(UUID first, UUID second) { return first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first; }
    public UUID getId() { return id; } public Type getType() { return type; } public UUID getGroupId() { return groupId; }
    public Instant getCreatedAt() { return createdAt; }
}
