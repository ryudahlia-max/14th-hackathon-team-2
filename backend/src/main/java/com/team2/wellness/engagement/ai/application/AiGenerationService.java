package com.team2.wellness.engagement.ai.application;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import com.team2.wellness.engagement.ai.persistence.AiGenerationJobRepository;
import com.team2.wellness.engagement.chat.application.ChatService;
import com.team2.wellness.engagement.chat.domain.ChatMessage;
import com.team2.wellness.engagement.notification.application.NotificationService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.FaceReferenceValidationPort;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiGenerationService {

    private final AiGenerationJobRepository jobs;
    private final CoreAccessPort core;
    private final MediaStoragePort storage;
    private final FaceReferenceValidationPort faceReferences;
    private final ImageGenerationPort images;
    private final ChatService chat;
    private final NotificationService notifications;
    private final RealtimePublisherPort realtime;
    private final SafeFuturePromptBuilder prompts;
    private final TransactionTemplate tx;
    private final int dailyLimit;

    public AiGenerationService(
            AiGenerationJobRepository jobs,
            CoreAccessPort core,
            MediaStoragePort storage,
            FaceReferenceValidationPort faceReferences,
            ImageGenerationPort images,
            ChatService chat,
            NotificationService notifications,
            RealtimePublisherPort realtime,
            SafeFuturePromptBuilder prompts,
            TransactionTemplate tx,
            @Value("${app.ai.daily-limit:3}") int dailyLimit
    ) {
        this.jobs = jobs;
        this.core = core;
        this.storage = storage;
        this.faceReferences = faceReferences;
        this.images = images;
        this.chat = chat;
        this.notifications = notifications;
        this.realtime = realtime;
        this.prompts = prompts;
        this.tx = tx;
        this.dailyLimit = dailyLimit;
    }

    public AiGenerationJob request(UUID requester, UUID target, UUID occurrence, String clientRequestId) {
        Optional<AiGenerationJob> existing = jobs.findByRequesterIdAndClientRequestId(requester, clientRequestId);
        if (existing.isPresent()) return existing.get();
        if (!core.areAcceptedFriends(requester, target)) {
            throw fail(HttpStatus.FORBIDDEN, "AI_REQUIRES_FRIENDSHIP", "친구 관계에서만 만들 수 있습니다.");
        }
        if (!core.hasAiImageConsent(target)) {
            throw fail(HttpStatus.FORBIDDEN, "AI_CONSENT_REQUIRED", "친구가 AI 얼굴 사진 사용에 동의해야 합니다.");
        }
        if (core.getMissedRoutineOccurrence(occurrence, target).isEmpty()) {
            throw fail(HttpStatus.BAD_REQUEST, "MISSED_OCCURRENCE_REQUIRED", "생성 가능한 미완료 루틴이 아닙니다.");
        }
        validateFaceReference(target, true);

        return tx.execute(status -> {
            Optional<AiGenerationJob> duplicate = jobs.findByRequesterIdAndClientRequestId(requester, clientRequestId);
            if (duplicate.isPresent()) return duplicate.get();
            Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            if (dailyLimit > 0
                    && jobs.countByRequesterIdAndCreatedAtGreaterThanEqual(requester, startOfDay) >= dailyLimit) {
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
            FaceReference face = validateFaceReference(claimed.getTargetUserId(), false);
            ImageGenerationPort.ImageResult result = images.generate(new ImageGenerationPort.ImageCommand(
                    prompts.build(occurrence),
                    face.bytes(),
                    face.media().contentType()
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

    private FaceReference validateFaceReference(UUID targetUserId, boolean apiRequest) {
        MediaStoragePort.StoredMedia media = storage.findFaceAsset(targetUserId)
                .orElseThrow(() -> referenceFailure("FACE_ASSET_MISSING", apiRequest));
        byte[] bytes = storage.read(media.objectKey());
        try {
            if (!faceReferences.isUsableIdentityReference(bytes, media.contentType())) {
                throw referenceFailure("FACE_REFERENCE_INVALID", apiRequest);
            }
            return new FaceReference(media, bytes);
        } catch (ImageGenerationException exception) {
            if (!apiRequest) throw exception;
            HttpStatus status = exception.retryable()
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.BAD_GATEWAY;
            throw fail(status, exception.code(), "얼굴 사진 확인 서비스에 일시적인 문제가 있습니다.");
        }
    }

    private RuntimeException referenceFailure(String code, boolean apiRequest) {
        if (!apiRequest) return new NonRetryable(code);
        String message = "FACE_ASSET_MISSING".equals(code)
                ? "친구가 프로필 사진을 등록해야 합니다."
                : "친구의 프로필을 한 명의 얼굴이 선명한 실제 사진으로 바꾼 뒤 다시 시도해 주세요.";
        return fail(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    private ApiException fail(HttpStatus status, String code) {
        return fail(status, code, code);
    }

    private ApiException fail(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }

    private record FaceReference(MediaStoragePort.StoredMedia media, byte[] bytes) {
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
