package com.team2.wellness.engagement.ai.api;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.ai.application.AiGenerationService;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import com.team2.wellness.engagement.port.out.CurrentUserPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/engagement/ai-generations")
public class AiGenerationController {

    private final CurrentUserPort user;
    private final AiGenerationService ai;
    private final MediaStoragePort storage;

    public AiGenerationController(CurrentUserPort user, AiGenerationService ai, MediaStoragePort storage) {
        this.user = user;
        this.ai = ai;
        this.storage = storage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    JobResponse request(@Valid @RequestBody Request request) {
        return response(ai.request(
                user.currentUserId(),
                request.targetUserId(),
                request.occurrenceId(),
                request.clientRequestId()
        ));
    }

    @GetMapping("/{jobId}")
    JobResponse get(@PathVariable UUID jobId) {
        return ai.get(user.currentUserId(), jobId)
                .map(this::response)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "AI_JOB_NOT_FOUND",
                        "AI job not found"
                ));
    }

    private JobResponse response(AiGenerationJob job) {
        String outputUrl = null;
        if (job.getStatus() == AiGenerationJob.Status.SUCCEEDED && job.getOutputObjectKey() != null) {
            outputUrl = storage.temporaryDownloadUrl(job.getOutputObjectKey());
        }
        return new JobResponse(
                job.getId(),
                job.getStatus(),
                job.getAttemptCount(),
                outputUrl,
                job.getFailureCode()
        );
    }

    record Request(@NotNull UUID targetUserId, @NotNull UUID occurrenceId, @NotBlank String clientRequestId) {
    }

    record JobResponse(
            UUID id,
            AiGenerationJob.Status status,
            int attemptCount,
            String outputUrl,
            String failureCode
    ) {
    }
}
