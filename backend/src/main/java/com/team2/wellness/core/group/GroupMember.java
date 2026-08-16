package com.team2.wellness.core.group;

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
        name = "group_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"})
)
public class GroupMember {

    public enum Role {
        OWNER, MEMBER
    }

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected GroupMember() {
    }

    public GroupMember(UUID groupId, UUID userId, Role role) {
        this.id = UUID.randomUUID();
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }
}
