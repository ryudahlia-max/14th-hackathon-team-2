package com.team2.wellness.core.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

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
    void createKeepsSemanticCategorySeparateFromDisplayColor() {
        UUID userId = UUID.randomUUID();
        when(routineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RoutineService.RoutineCommand command = new RoutineService.RoutineCommand(
                "물 마시기",
                "HYDRATION",
                "#34D399",
                EnumSet.allOf(DayOfWeek.class),
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                "Asia/Seoul",
                LocalDate.of(2026, 8, 1),
                null
        );

        service.create(userId, command);

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("HYDRATION");
        assertThat(captor.getValue().getColor()).isEqualTo("#34D399");
        assertThat(captor.getValue().getCompletionDeadline()).isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    void rejectsCompletionOnUnscheduledDay() {
        UUID userId = UUID.randomUUID();
        Routine routine = new Routine(
                userId,
                "주말 산책",
                "MOVEMENT",
                "#60A5FA",
                EnumSet.of(DayOfWeek.SATURDAY),
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
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
        assertThat(day.completedRoutineIds()).containsExactly(routine.getId());
    }

    @Test
    void ownerCanDeleteRoutine() {
        UUID userId = UUID.randomUUID();
        Routine routine = dailyRoutine(userId);
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));

        service.delete(userId, routine.getId());

        verify(routineRepository).delete(routine);
    }

    @Test
    void uncompleteDeletesOnlyOwnedRoutineCompletion() {
        UUID userId = UUID.randomUUID();
        Routine routine = dailyRoutine(userId);
        LocalDate date = LocalDate.of(2026, 8, 18);
        RoutineCompletion completion = new RoutineCompletion(routine.getId(), userId, date, java.time.Instant.now(), null, null);
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));
        when(completionRepository.findByRoutineIdAndCompletionDate(routine.getId(), date)).thenReturn(Optional.of(completion));

        service.uncomplete(userId, routine.getId(), date);

        verify(completionRepository).delete(completion);
    }

    @Test
    void missedRoutineIncludesTypeCountAndMostRecentDate() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 20);
        Routine routine = new Routine(
                userId,
                "아침 물 마시기",
                "수분",
                "#60A5FA",
                EnumSet.allOf(DayOfWeek.class),
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                "Asia/Seoul",
                today.minusDays(5),
                null
        );
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));
        when(completionRepository.findAllByRoutineIdAndCompletionDateBetween(
                routine.getId(),
                today.minusDays(5),
                today.minusDays(1)
        )).thenReturn(List.of(
                new RoutineCompletion(routine.getId(), userId, today.minusDays(2), java.time.Instant.now(), null, null),
                new RoutineCompletion(routine.getId(), userId, today.minusDays(4), java.time.Instant.now(), null, null)
        ));

        assertThat(service.missedRoutine(
                userId,
                routine.getId(),
                java.time.Instant.parse("2026-08-20T11:00:00Z")
        ))
                .hasValueSatisfying(missed -> {
                    assertThat(missed.title()).isEqualTo("아침 물 마시기");
                    assertThat(missed.category()).isEqualTo("수분");
                    assertThat(missed.missedCount()).isEqualTo(3);
                    assertThat(missed.missedDate()).isEqualTo(today.minusDays(1));
                });
    }

    @Test
    void todaysRoutineBecomesMissedAtItsLocalCompletionDeadline() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 20);
        Routine routine = new Routine(
                userId,
                "데모 산책",
                "MOVEMENT",
                "#60A5FA",
                EnumSet.of(DayOfWeek.THURSDAY),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                "Asia/Seoul",
                today,
                null
        );
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));
        when(completionRepository.findAllByRoutineIdAndCompletionDateBetween(routine.getId(), today, today))
                .thenReturn(List.of());

        assertThat(service.missedRoutine(
                userId,
                routine.getId(),
                java.time.Instant.parse("2026-08-20T01:00:00Z")
        )).hasValueSatisfying(missed -> {
            assertThat(missed.missedDate()).isEqualTo(today);
            assertThat(missed.missedCount()).isEqualTo(1);
        });
    }

    @Test
    void todaysRoutineIsNotMissedBeforeItsLocalCompletionDeadline() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 20);
        Routine routine = new Routine(
                userId,
                "데모 산책",
                "MOVEMENT",
                "#60A5FA",
                EnumSet.of(DayOfWeek.THURSDAY),
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                "Asia/Seoul",
                today,
                null
        );
        when(routineRepository.findByIdAndOwnerId(routine.getId(), userId)).thenReturn(Optional.of(routine));

        assertThat(service.missedRoutine(
                userId,
                routine.getId(),
                java.time.Instant.parse("2026-08-20T00:59:59Z")
        )).isEmpty();
    }

    private Routine dailyRoutine(UUID userId) {
        return new Routine(
                userId,
                "물 마시기",
                "WATER",
                "#60A5FA",
                EnumSet.allOf(DayOfWeek.class),
                LocalTime.of(9, 0),
                LocalTime.of(21, 0),
                "Asia/Seoul",
                LocalDate.of(2026, 8, 1),
                null
        );
    }
}
