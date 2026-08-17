package com.team2.wellness.core.routine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "routine_completions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"routine_id", "completion_date"})
)
public class RoutineCompletion {

    @Id
    private UUID id;

    @Column(name = "routine_id", nullable = false)
    private UUID routineId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "proof_object_path", length = 500)
    private String proofObjectPath;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoutineCompletion() {
    }

    public RoutineCompletion(
            UUID routineId,
            UUID userId,
            LocalDate completionDate,
            Instant completedAt,
            String proofObjectPath,
            String note
    ) {
        this.id = UUID.randomUUID();
        this.routineId = routineId;
        this.userId = userId;
        this.completionDate = completionDate;
        this.completedAt = completedAt;
        this.proofObjectPath = proofObjectPath;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getProofObjectPath() {
        return proofObjectPath;
    }

    public String getNote() {
        return note;
    }
}
