package com.team2.wellness.engagement.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import com.team2.wellness.engagement.ai.persistence.AiGenerationJobRepository;
import com.team2.wellness.engagement.chat.application.ChatService;
import com.team2.wellness.engagement.chat.domain.ChatRoom;
import com.team2.wellness.engagement.notification.application.NotificationService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.ImageGenerationPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AiGenerationServiceTest {

    private final AiGenerationJobRepository jobs = mock(AiGenerationJobRepository.class);
    private final CoreAccessPort core = mock(CoreAccessPort.class);
    private final MediaStoragePort storage = mock(MediaStoragePort.class);
    private final ImageGenerationPort images = mock(ImageGenerationPort.class);
    private final ChatService chat = mock(ChatService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final UUID requester = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final UUID occurrence = UUID.randomUUID();
    private AiGenerationService service;

    @BeforeEach
    void setUp() {
        service = new AiGenerationService(
                jobs,
                core,
                storage,
                images,
                chat,
                notifications,
                realtime,
                new SafeFuturePromptBuilder(),
                new TransactionTemplate(new NoopTransactionManager())
        );
        when(core.areAcceptedFriends(requester, target)).thenReturn(true);
        when(core.hasAiImageConsent(target)).thenReturn(true);
        when(core.getMissedRoutineOccurrence(occurrence, target)).thenReturn(Optional.of(
                new CoreAccessPort.MissedRoutineOccurrence(
                        occurrence,
                        UUID.randomUUID(),
                        target,
                        "물 마시기",
                        "HYDRATION",
                        5,
                        LocalDate.of(2026, 8, 18)
                )
        ));
    }

    @Test
    void consentOrMissingOccurrenceBlocksRequest() {
        when(core.hasAiImageConsent(target)).thenReturn(false);

        assertThatThrownBy(() -> service.request(requester, target, occurrence, "x"))
                .isInstanceOf(ApiException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void dailyLimitIsEnforced() {
        when(jobs.countByRequesterIdAndCreatedAtGreaterThanEqual(eq(requester), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.request(requester, target, occurrence, "x"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void revokedConsentBeforeWorkerExecutionBlocksWithoutCallingProvider() {
        AiGenerationJob job = readyJob();
        when(core.hasAiImageConsent(target)).thenReturn(false);

        service.processNext();

        assertThat(job.getStatus()).isEqualTo(AiGenerationJob.Status.BLOCKED);
        assertThat(job.getFailureCode()).isEqualTo("AI_CONSENT_REVOKED");
        verifyNoInteractions(images);
    }

    @Test
    void consentRevokedDuringProviderCallPreventsStorageAndChat() {
        AiGenerationJob job = readyJob();
        prepareFace();
        when(images.generate(any())).thenAnswer(invocation -> {
            when(core.hasAiImageConsent(target)).thenReturn(false);
            return new ImageGenerationPort.ImageResult(new byte[]{2}, "image/png");
        });

        service.processNext();

        assertThat(job.getStatus()).isEqualTo(AiGenerationJob.Status.BLOCKED);
        verify(storage, never()).storeAiOutput(any(), any(), anyString());
        verifyNoInteractions(chat);
    }

    @Test
    void provider429IsQueuedForRetry() {
        AiGenerationJob job = readyJob();
        prepareFace();
        when(images.generate(any())).thenThrow(new ImageGenerationException("OPENAI_RATE_LIMIT"));

        service.processNext();

        assertThat(job.getStatus()).isEqualTo(AiGenerationJob.Status.QUEUED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void staleRunningJobIsRecoveredAfterWorkerRestart() {
        AiGenerationJob job = new AiGenerationJob(requester, target, occurrence, "stale");
        assertThat(job.claim(Instant.now().plusSeconds(1))).isTrue();
        when(jobs.findStaleRunningForUpdate(any(), any())).thenReturn(List.of(job));

        assertThat(service.recoverStaleRunning(Duration.ofMinutes(10))).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(AiGenerationJob.Status.QUEUED);
        assertThat(job.getFailureCode()).isEqualTo("WORKER_INTERRUPTED");
    }

    @Test
    void successCreatesRoutineAwarePromptChatMessageAndNotification() {
        AiGenerationJob job = readyJob();
        prepareFace();
        when(images.generate(any())).thenReturn(new ImageGenerationPort.ImageResult(new byte[]{2}, "image/png"));
        when(storage.storeAiOutput(eq(target), any(), eq("image/png")))
                .thenReturn(new MediaStoragePort.StoredMedia("out", "image/png"));
        when(chat.createDirect(requester, target)).thenReturn(ChatRoom.direct(requester, target));

        service.processNext();

        assertThat(job.getStatus()).isEqualTo(AiGenerationJob.Status.SUCCEEDED);
        verify(images).generate(argThat(command ->
                command.prompt().contains("<title>물 마시기</title>")
                        && command.prompt().contains("<category>HYDRATION</category>")
                        && command.prompt().contains("<missed_occurrences_last_366_scheduled_days>5")));
        verify(chat).send(eq(requester), any(), argThat(command -> command.content().contains("최근 5번")));
        verify(notifications).createOnce(
                eq(target), eq("AI_COMPLETED"), anyString(), argThat(key -> key.startsWith("ai-completed:")));
    }

    private AiGenerationJob readyJob() {
        AiGenerationJob job = new AiGenerationJob(requester, target, occurrence, UUID.randomUUID().toString());
        when(jobs.findReady(any(), any())).thenReturn(List.of(job));
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    private void prepareFace() {
        when(storage.findFaceAsset(target))
                .thenReturn(Optional.of(new MediaStoragePort.StoredMedia("face", "image/png")));
        when(storage.read("face")).thenReturn(new byte[]{1});
    }

    static class NoopTransactionManager implements PlatformTransactionManager {
        public TransactionStatus getTransaction(TransactionDefinition definition) { return new SimpleTransactionStatus(); }
        public void commit(TransactionStatus status) { }
        public void rollback(TransactionStatus status) { }
    }
}
