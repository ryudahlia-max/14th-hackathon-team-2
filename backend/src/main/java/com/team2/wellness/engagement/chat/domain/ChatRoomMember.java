package com.team2.wellness.engagement.chat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "chat_room_members", uniqueConstraints = @UniqueConstraint(name = "uk_chat_room_members_room_user", columnNames = {"room_id", "user_id"}))
public class ChatRoomMember {
    @Id private UUID id;
    @Column(name = "room_id", nullable = false) private UUID roomId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "joined_at", nullable = false, updatable = false) private Instant joinedAt;
    protected ChatRoomMember() { }
    public ChatRoomMember(UUID roomId, UUID userId) { this.id = UUID.randomUUID(); this.roomId = roomId; this.userId = userId; this.joinedAt = Instant.now(); }
    public UUID getRoomId() { return roomId; } public UUID getUserId() { return userId; }
}
