package com.team2.wellness.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team2.wellness.core.group.GroupMemberRepository;
import com.team2.wellness.core.group.WellnessGroupRepository;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import com.team2.wellness.engagement.recap.application.MonthlyRecapService;
import java.time.Instant;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class MonthlyRecapSchedulerTest {

    @Test
    void previousMonthUsesConfiguredSeoulTimezoneAtUtcMonthBoundary() {
        MonthlyRecapScheduler scheduler = new MonthlyRecapScheduler(
                mock(WellnessGroupRepository.class),
                mock(GroupMemberRepository.class),
                mock(RoutineRepository.class),
                mock(RoutineCompletionRepository.class),
                mock(MonthlyRecapService.class),
                "Asia/Seoul"
        );

        YearMonth result = scheduler.previousMonth(Instant.parse("2026-08-31T15:05:00Z"));

        assertThat(result).isEqualTo(YearMonth.of(2026, 8));
    }
}
