package com.team2.wellness.engagement.recap.application;

import com.team2.wellness.engagement.notification.application.NotificationService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.ImageGenerationPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import com.team2.wellness.engagement.recap.domain.MonthlyRecap;
import com.team2.wellness.engagement.recap.persistence.MonthlyRecapRepository;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MonthlyRecapService {

    private final MonthlyRecapRepository recaps;
    private final CoreAccessPort core;
    private final ImageGenerationPort images;
    private final MediaStoragePort storage;
    private final NotificationService notifications;
    private final TransactionTemplate transaction;

    public MonthlyRecapService(
            MonthlyRecapRepository recaps,
            CoreAccessPort core,
            ImageGenerationPort images,
            MediaStoragePort storage,
            NotificationService notifications,
            TransactionTemplate transaction
    ) {
        this.recaps = recaps;
        this.core = core;
        this.images = images;
        this.storage = storage;
        this.notifications = notifications;
        this.transaction = transaction;
    }

    public MonthlyRecap create(MonthlyGroupStats stats) {
        AtomicBoolean created = new AtomicBoolean(false);
        MonthlyRecap recap;
        try {
            recap = transaction.execute(status -> recaps.findByGroupIdAndRecapMonth(stats.groupId(), stats.month())
                    .orElseGet(() -> {
                        created.set(true);
                        return recaps.saveAndFlush(new MonthlyRecap(
                                stats.groupId(),
                                stats.month(),
                                positiveSummary(stats)
                        ));
                    }));
        } catch (DataIntegrityViolationException race) {
            return recaps.findByGroupIdAndRecapMonth(stats.groupId(), stats.month()).orElseThrow(() -> race);
        }

        if (!created.get()) {
            return recap;
        }

        attachGeneratedImage(recap);
        core.getGroupMemberIds(stats.groupId()).forEach(member ->
                notifications.create(member, "MONTHLY_RECAP", recap.getSummary()));
        return recaps.findById(recap.getId()).orElse(recap);
    }

    private void attachGeneratedImage(MonthlyRecap recap) {
        try {
            var image = images.generate(new ImageGenerationPort.ImageCommand(
                    "A positive, calm Korean wellness group monthly recap illustration. No text or logos.",
                    new byte[0],
                    "image/png"
            ));
            var stored = storage.storeAiOutput(recap.getGroupId(), image.bytes(), image.contentType());
            transaction.executeWithoutResult(status -> recaps.findById(recap.getId())
                    .ifPresent(saved -> saved.attachImage(stored.objectKey())));
        } catch (RuntimeException ignored) {
            // The persisted recap and in-app notifications remain available without generated artwork.
        }
    }

    private String positiveSummary(MonthlyGroupStats stats) {
        return "이번 달, 함께 " + stats.completedCount()
                + "번의 루틴을 완주했어요. 다음 달도 서로를 응원해요!";
    }

    public record MonthlyGroupStats(UUID groupId, YearMonth month, long completedCount, long totalCount) {
        public MonthlyGroupStats {
            Objects.requireNonNull(groupId);
            Objects.requireNonNull(month);
            if (completedCount < 0 || totalCount < 0) {
                throw new IllegalArgumentException("Counts must be non-negative");
            }
        }
    }
}
