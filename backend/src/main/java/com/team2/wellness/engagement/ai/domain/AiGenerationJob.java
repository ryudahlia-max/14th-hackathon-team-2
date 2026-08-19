package com.team2.wellness.engagement.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_generation_jobs", uniqueConstraints = @UniqueConstraint(name = "uk_ai_jobs_requester_client", columnNames = {"requester_id", "client_request_id"}))
public class AiGenerationJob {
    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED, BLOCKED }
    @Id private UUID id;
    @Column(name = "requester_id", nullable = false) private UUID requesterId;
    @Column(name = "target_user_id", nullable = false) private UUID targetUserId;
    @Column(name = "occurrence_id", nullable = false) private UUID occurrenceId;
    @Column(name = "client_request_id", nullable = false, length = 100) private String clientRequestId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "output_object_key") private String outputObjectKey;
    @Column(name = "failure_code", length = 64) private String failureCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected AiGenerationJob() { }
    public AiGenerationJob(UUID requesterId, UUID targetUserId, UUID occurrenceId, String clientRequestId) { this.id=UUID.randomUUID(); this.requesterId=requesterId; this.targetUserId=targetUserId; this.occurrenceId=occurrenceId; this.clientRequestId=clientRequestId; this.status=Status.QUEUED; this.nextAttemptAt=Instant.now(); this.createdAt=Instant.now(); this.updatedAt=createdAt; }
    public boolean claim(Instant now) { if (status != Status.QUEUED || nextAttemptAt.isAfter(now)) return false; status=Status.RUNNING; attemptCount++; updatedAt=now; return true; }
    public void succeed(String objectKey) { status=Status.SUCCEEDED; outputObjectKey=objectKey; updatedAt=Instant.now(); }
    public void block(String code) { status=Status.BLOCKED; failureCode=code; updatedAt=Instant.now(); }
    public void retryOrFail(String code) { failureCode=code; updatedAt=Instant.now(); if (attemptCount >= 3) status=Status.FAILED; else { status=Status.QUEUED; nextAttemptAt=Instant.now().plusSeconds(1L << attemptCount); } }
    public boolean recoverStaleRun(Instant now) { if (status != Status.RUNNING) return false; failureCode="WORKER_INTERRUPTED"; updatedAt=now; if (attemptCount >= 3) status=Status.FAILED; else { status=Status.QUEUED; nextAttemptAt=now; } return true; }
    public UUID getId(){return id;} public UUID getRequesterId(){return requesterId;} public UUID getTargetUserId(){return targetUserId;} public UUID getOccurrenceId(){return occurrenceId;} public Status getStatus(){return status;} public int getAttemptCount(){return attemptCount;} public Instant getNextAttemptAt(){return nextAttemptAt;} public String getOutputObjectKey(){return outputObjectKey;} public String getFailureCode(){return failureCode;} public Instant getUpdatedAt(){return updatedAt;}
}
