package com.team2.wellness.engagement.ai.application;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import com.team2.wellness.engagement.ai.persistence.AiGenerationJobRepository;
import com.team2.wellness.engagement.chat.application.ChatService;
import com.team2.wellness.engagement.chat.domain.ChatMessage;
import com.team2.wellness.engagement.notification.application.NotificationService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.ImageGenerationPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiGenerationService {

    private final AiGenerationJobRepository jobs;
    private final CoreAccessPort core;
    private final MediaStoragePort storage;
    private final ImageGenerationPort images;
    private final ChatService chat;
    private final NotificationService notifications;
    private final RealtimePublisherPort realtime;
    private final SafeFuturePromptBuilder prompts;
    private final TransactionTemplate tx;

    public AiGenerationService(
            AiGenerationJobRepository jobs,
            CoreAccessPort core,
            MediaStoragePort storage,
            ImageGenerationPort images,
            ChatService chat,
            NotificationService notifications,
            RealtimePublisherPort realtime,
            SafeFuturePromptBuilder prompts,
            TransactionTemplate tx
    ) {
        this.jobs = jobs;
        this.core = core;
        this.storage = storage;
        this.images = images;
        this.chat = chat;
        this.notifications = notifications;
        this.realtime = realtime;
        this.prompts = prompts;
        this.tx = tx;
    }

    public AiGenerationJob request(UUID requester, UUID target, UUID occurrence, String clientRequestId) {
        return tx.execute(status -> {
            Optional<AiGenerationJob> existing = jobs.findByRequesterIdAndClientRequestId(requester, clientRequestId);
            if (existing.isPresent()) return existing.get();
            if (!core.areAcceptedFriends(requester, target)) {
                throw fail(HttpStatus.FORBIDDEN, "AI_REQUIRES_FRIENDSHIP");
            }
            if (!core.hasAiImageConsent(target)) {
                throw fail(HttpStatus.FORBIDDEN, "AI_CONSENT_REQUIRED");
            }
            if (core.getMissedRoutineOccurrence(occurrence, target).isEmpty()) {
                throw fail(HttpStatus.BAD_REQUEST, "MISSED_OCCURRENCE_REQUIRED");
            }
            Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            if (jobs.countByRequesterIdAndCreatedAtGreaterThanEqual(requester, startOfDay) >= 3) {
                throw fail(HttpStatus.TOO_MANY_REQUESTS, "AI_DAILY_LIMIT");
            }
            return jobs.save(new AiGenerationJob(requester, target, occurrence, clientRequestId));
        });
    }

    public Optional<AiGenerationJob> processNext() {
        AiGenerationJob claimed = tx.execute(status -> jobs.findReady(Instant.now(), PageRequest.of(0, 1)).stream()
                .filter(job -> job.claim(Instant.now()))
                .findFirst()
                .orElse(null));
        if (claimed == null) return Optional.empty();

        try {
            validateRelationshipAndConsent(claimed);
            CoreAccessPort.MissedRoutineOccurrence occurrence = core
                    .getMissedRoutineOccurrence(claimed.getOccurrenceId(), claimed.getTargetUserId())
                    .orElseThrow(() -> new NonRetryable("MISSED_OCCURRENCE_NO_LONGER_AVAILABLE"));
            MediaStoragePort.StoredMedia face = storage.findFaceAsset(claimed.getTargetUserId())
                    .orElseThrow(() -> new NonRetryable("FACE_ASSET_MISSING"));
            ImageGenerationPort.ImageResult result = images.generate(new ImageGenerationPort.ImageCommand(
                    prompts.build(occurrence),
                    storage.read(face.objectKey()),
                    face.contentType()
            ));
            validateRelationshipAndConsent(claimed);
            MediaStoragePort.StoredMedia stored = storage.storeAiOutput(
                    claimed.getTargetUserId(),
                    result.bytes(),
                    result.contentType()
            );
            tx.executeWithoutResult(status -> complete(claimed.getId(), occurrence, stored.objectKey()));
        } catch (NonRetryable exception) {
            tx.executeWithoutResult(status -> jobs.findById(claimed.getId())
                    .ifPresent(job -> job.block(exception.getMessage())));
        } catch (ImageGenerationException exception) {
            tx.executeWithoutResult(status -> jobs.findById(claimed.getId()).ifPresent(job -> {
                if (exception.retryable()) job.retryOrFail(exception.code());
                else job.block(exception.code());
            }));
        } catch (RuntimeException exception) {
            tx.executeWithoutResult(status -> jobs.findById(claimed.getId())
                    .ifPresent(job -> job.retryOrFail(code(exception))));
        }
        return jobs.findById(claimed.getId());
    }

    public int recoverStaleRunning(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("AI running timeout must be positive");
        }
        Integer recovered = tx.execute(status -> {
            Instant now = Instant.now();
            return (int) jobs.findStaleRunningForUpdate(now.minus(timeout), PageRequest.of(0, 100)).stream()
                    .filter(job -> job.recoverStaleRun(now))
                    .count();
        });
        return recovered == null ? 0 : recovered;
    }

    private void complete(
            UUID jobId,
            CoreAccessPort.MissedRoutineOccurrence occurrence,
            String outputObjectKey
    ) {
        AiGenerationJob job = jobs.findById(jobId).orElseThrow();
        validateRelationshipAndConsent(job);
        job.succeed(outputObjectKey);
        var room = chat.createDirect(job.getRequesterId(), job.getTargetUserId());
        String message = "%s 루틴을 최근 %d번 놓친 흐름을 반영한 미래 이미지예요."
                .formatted(occurrence.routineTitle(), occurrence.missedCount());
        chat.send(job.getRequesterId(), room.getId(), new ChatService.SendCommand(
                "ai:" + job.getId(),
                ChatMessage.Type.AI_IMAGE,
                message,
                outputObjectKey
        ));
        notifications.createOnce(
                job.getTargetUserId(),
                "AI_COMPLETED",
                "새로운 미래 이미지가 도착했어요.",
                "ai-completed:" + job.getId() + ":" + job.getTargetUserId()
        );
        publish("ai-generation:" + job.getId(), "ai_generation.succeeded", new JobView(job));
    }

    public Optional<AiGenerationJob> get(UUID requester, UUID jobId) {
        return jobs.findById(jobId).filter(job -> job.getRequesterId().equals(requester));
    }

    private void publish(String topic, String type, Object payload) {
        try {
            realtime.publish(topic, type, payload);
        } catch (RuntimeException ignored) {
        }
    }

    private void validateRelationshipAndConsent(AiGenerationJob job) {
        if (!core.areAcceptedFriends(job.getRequesterId(), job.getTargetUserId())) {
            throw new NonRetryable("AI_FRIENDSHIP_REVOKED");
        }
        if (!core.hasAiImageConsent(job.getTargetUserId())) {
            throw new NonRetryable("AI_CONSENT_REVOKED");
        }
    }

    private String code(RuntimeException exception) {
        return exception instanceof ImageGenerationException imageException
                ? imageException.code()
                : "AI_PROVIDER_ERROR";
    }

    private ApiException fail(HttpStatus status, String code) {
        return new ApiException(status, code, code);
    }

    public record JobView(
            UUID id,
            AiGenerationJob.Status status,
            int attemptCount,
            String outputObjectKey,
            String failureCode
    ) {
        public JobView(AiGenerationJob job) {
            this(job.getId(), job.getStatus(), job.getAttemptCount(), job.getOutputObjectKey(), job.getFailureCode());
        }
    }

    static class NonRetryable extends RuntimeException {
        NonRetryable(String message) {
            super(message);
        }
    }
}
