package com.team2.wellness.infrastructure.scheduling;

import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineRepository;
import com.team2.wellness.engagement.notification.application.NotificationService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.reminder", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoutineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoutineReminderScheduler.class);

    private final RoutineRepository routines;
    private final NotificationService notifications;

    public RoutineReminderScheduler(RoutineRepository routines, NotificationService notifications) {
        this.routines = routines;
        this.notifications = notifications;
    }

    @Scheduled(cron = "${app.reminder.cron:0 * * * * *}", zone = "UTC")
    public void createDueReminders() {
        createDueReminders(Instant.now());
    }

    int createDueReminders(Instant now) {
        int created = 0;
        for (Routine routine : routines.findAllByActiveTrue()) {
            try {
                ZonedDateTime localNow = now.atZone(ZoneId.of(routine.getTimezone()));
                if (!routine.isScheduledOn(localNow.toLocalDate())
                        || routine.getReminderTime().getHour() != localNow.getHour()
                        || routine.getReminderTime().getMinute() != localNow.getMinute()) {
                    continue;
                }
                String key = "routine-reminder:" + routine.getId() + ":" + localNow.toLocalDate();
                notifications.createOnce(
                        routine.getOwnerId(),
                        "ROUTINE_REMINDER",
                        "‘" + routine.getTitle() + "’ 루틴을 실천할 시간이에요.",
                        key
                );
                created++;
            } catch (RuntimeException exception) {
                log.warn("Routine reminder failed for routine {}", routine.getId(), exception);
            }
        }
        return created;
    }
}
