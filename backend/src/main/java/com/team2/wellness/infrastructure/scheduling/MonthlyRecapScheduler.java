package com.team2.wellness.infrastructure.scheduling;

import com.team2.wellness.core.group.GroupMemberRepository;
import com.team2.wellness.core.group.WellnessGroupRepository;
import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import com.team2.wellness.engagement.recap.application.MonthlyRecapService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.recap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyRecapScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyRecapScheduler.class);

    private final WellnessGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final RoutineRepository routineRepository;
    private final RoutineCompletionRepository completionRepository;
    private final MonthlyRecapService monthlyRecapService;

    public MonthlyRecapScheduler(
            WellnessGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            RoutineRepository routineRepository,
            RoutineCompletionRepository completionRepository,
            MonthlyRecapService monthlyRecapService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.routineRepository = routineRepository;
        this.completionRepository = completionRepository;
        this.monthlyRecapService = monthlyRecapService;
    }

    @Scheduled(cron = "${app.recap.cron:0 5 0 1 * *}", zone = "${app.recap.zone:Asia/Seoul}")
    public void createPreviousMonthRecaps() {
        YearMonth month = YearMonth.now().minusMonths(1);
        groupRepository.findAll().forEach(group -> {
            try {
                monthlyRecapService.create(statistics(group.getId(), month));
            } catch (RuntimeException exception) {
                log.error("Monthly recap failed for group {} and month {}", group.getId(), month, exception);
            }
        });
    }

    MonthlyRecapService.MonthlyGroupStats statistics(UUID groupId, YearMonth month) {
        List<UUID> memberIds = memberRepository.findAllByGroupId(groupId).stream()
                .map(member -> member.getUserId())
                .distinct()
                .toList();
        if (memberIds.isEmpty()) {
            return new MonthlyRecapService.MonthlyGroupStats(groupId, month, 0, 0);
        }

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        long completed = completionRepository.countByUserIdInAndCompletionDateBetween(memberIds, start, end);
        List<Routine> routines = routineRepository.findAllByOwnerIdInAndActiveTrue(memberIds);
        long scheduled = routines.stream().mapToLong(routine -> scheduledDays(routine, start, end)).sum();
        return new MonthlyRecapService.MonthlyGroupStats(groupId, month, completed, scheduled);
    }

    private long scheduledDays(Routine routine, LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (routine.isScheduledOn(date)) {
                count++;
            }
        }
        return count;
    }
}
