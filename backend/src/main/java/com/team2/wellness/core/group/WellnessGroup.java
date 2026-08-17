package com.team2.wellness.core.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wellness_groups")
public class WellnessGroup {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "max_members", nullable = false)
    private short maxMembers;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WellnessGroup() {
    }

    public WellnessGroup(UUID ownerId, String name, short maxMembers) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name;
        this.maxMembers = maxMembers;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void rename(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public short getMaxMembers() {
        return maxMembers;
    }
}
