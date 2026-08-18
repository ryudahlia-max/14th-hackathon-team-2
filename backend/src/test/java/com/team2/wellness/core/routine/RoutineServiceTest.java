package com.team2.wellness.core.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoutineServiceTest {

    private RoutineRepository routineRepository;
    private RoutineCompletionRepository completionRepository;
    private RoutineService service;

    @BeforeEach
    void setUp() {
        routineRepository = mock(RoutineRepository.class);
        completionRepository = mock(RoutineCompletionRepository.class);
        service = new RoutineService(routineRepository, completionRepository);
    }

    @Test
    void completionIsIdempotentForRoutineAndDate() {
        UUID userId = UUID.randomUUID();
        Routine routine = dailyRoutine(userId);
        LocalDate date = LocalDate.of(2026, 8, 16);
        RoutineCompletion existing = new RoutineCompletion(
                routine.getId(), userId, date, java.time.Instant.now(), null, null
        );
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));
        when(completionRepository.findByRoutineIdAndCompletionDate(routine.getId(), date))
                .thenReturn(Optional.of(existing));

        RoutineCompletion result = service.complete(userId, routine.getId(), date, null, null);

        assertThat(result).isSameAs(existing);
        verify(completionRepository, never()).save(any());
    }

    @Test
    void rejectsCompletionOnUnscheduledDay() {
        UUID userId = UUID.randomUUID();
        Routine routine = new Routine(
                userId,
                "주말 산책",
                "MOVEMENT",
                EnumSet.of(DayOfWeek.SATURDAY),
                LocalTime.of(9, 0),
                "Asia/Seoul",
                LocalDate.of(2026, 8, 1),
                null
        );
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));

        assertThatThrownBy(() -> service.complete(
                userId,
                routine.getId(),
                LocalDate.of(2026, 8, 17),
                null,
                null
        )).isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("ROUTINE_NOT_SCHEDULED");
    }

    @Test
    void calendarCountsScheduledAndCompletedRoutines() {
        UUID userId = UUID.randomUUID();
        Routine routine = dailyRoutine(userId);
        when(routineRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(routine));
        when(completionRepository.findAllByUserIdAndCompletionDateBetweenOrderByCompletionDateAsc(
                userId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).thenReturn(List.of(new RoutineCompletion(
                routine.getId(),
                userId,
                LocalDate.of(2026, 8, 16),
                java.time.Instant.now(),
                null,
                null
        )));

        List<RoutineService.CalendarDay> result = service.calendar(userId, YearMonth.of(2026, 8));

        RoutineService.CalendarDay day = result.get(15);
        assertThat(day.scheduledCount()).isEqualTo(1);
        assertThat(day.completedCount()).isEqualTo(1);
        assertThat(day.completionRate()).isEqualTo(100.0);
    }

    private Routine dailyRoutine(UUID userId) {
        return new Routine(
                userId,
                "물 마시기",
                "WATER",
                EnumSet.allOf(DayOfWeek.class),
                LocalTime.of(9, 0),
                "Asia/Seoul",
                LocalDate.of(2026, 8, 1),
                null
        );
    }
}
