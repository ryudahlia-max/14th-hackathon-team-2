package com.team2.wellness.core.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routine_completion_reactions")
public class RoutineCompletionReaction {

    @Id
    private UUID id;
    @Column(name = "completion_id", nullable = false)
    private UUID completionId;
    @Column(name = "routine_owner_id", nullable = false)
    private UUID routineOwnerId;
    @Column(name = "reactor_id", nullable = false)
    private UUID reactorId;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoutineCompletionReaction() {
    }

    public RoutineCompletionReaction(UUID completionId, UUID routineOwnerId, UUID reactorId, String type) {
        this.id = UUID.randomUUID();
        this.completionId = completionId;
        this.routineOwnerId = routineOwnerId;
        this.reactorId = reactorId;
        this.type = type;
        this.createdAt = Instant.now();
    }

    public void changeType(String type) {
        this.type = type;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCompletionId() { return completionId; }
    public UUID getRoutineOwnerId() { return routineOwnerId; }
    public UUID getReactorId() { return reactorId; }
    public String getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
