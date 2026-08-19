package com.team2.wellness.infrastructure.scheduling;

import com.team2.wellness.engagement.ai.application.AiGenerationService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationWorker.class);

    private final AiGenerationService aiGenerationService;
    private final Duration runningTimeout;

    public AiGenerationWorker(
            AiGenerationService aiGenerationService,
            @Value("${app.ai.worker.running-timeout:PT10M}") Duration runningTimeout
    ) {
        this.aiGenerationService = aiGenerationService;
        this.runningTimeout = runningTimeout;
    }

    @Scheduled(
            initialDelayString = "${app.ai.worker.initial-delay-ms:5000}",
            fixedDelayString = "${app.ai.worker.fixed-delay-ms:3000}"
    )
    public void processNext() {
        try {
            aiGenerationService.recoverStaleRunning(runningTimeout);
            aiGenerationService.processNext();
        } catch (RuntimeException exception) {
            log.error("AI generation worker iteration failed", exception);
        }
    }
}
