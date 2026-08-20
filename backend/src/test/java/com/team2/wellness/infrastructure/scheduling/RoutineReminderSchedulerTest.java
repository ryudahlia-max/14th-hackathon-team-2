package com.team2.wellness.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineRepository;
import com.team2.wellness.engagement.notification.application.NotificationService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutineReminderSchedulerTest {

    @Test
    void createsOneIdempotentReminderAtRoutineLocalTime() {
        RoutineRepository routines = mock(RoutineRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        UUID userId = UUID.randomUUID();
        Routine routine = new Routine(
                userId,
                "아침 물 마시기",
                "HYDRATION",
                "#60A5FA",
                EnumSet.of(DayOfWeek.THURSDAY),
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                "Asia/Seoul",
                LocalDate.of(2026, 8, 1),
                null
        );
        when(routines.findAllByActiveTrue()).thenReturn(List.of(routine));
        RoutineReminderScheduler scheduler = new RoutineReminderScheduler(routines, notifications);

        int due = scheduler.createDueReminders(Instant.parse("2026-08-20T00:00:15Z"));

        assertThat(due).isEqualTo(1);
        verify(notifications).createOnce(
                eq(userId),
                eq("ROUTINE_REMINDER"),
                argThat(content -> content.contains("아침 물 마시기")),
                eq("routine-reminder:" + routine.getId() + ":2026-08-20")
        );
    }
}
