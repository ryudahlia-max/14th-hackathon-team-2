package com.team2.wellness.core.routine;

import com.team2.wellness.common.api.ApiException;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final RoutineCompletionRepository completionRepository;

    public RoutineService(
            RoutineRepository routineRepository,
            RoutineCompletionRepository completionRepository
    ) {
        this.routineRepository = routineRepository;
        this.completionRepository = completionRepository;
    }

    public Routine create(UUID userId, RoutineCommand command) {
        validateCommand(command);
        return routineRepository.save(new Routine(
                userId,
                command.title().trim(),
                command.category().trim(),
                command.daysOfWeek(),
                command.reminderTime(),
                command.timezone(),
                command.startDate(),
                command.endDate()
        ));
    }

    public Routine update(UUID userId, UUID routineId, RoutineCommand command, boolean active) {
        validateCommand(command);
        Routine routine = ownedRoutine(userId, routineId);
        routine.update(
                command.title().trim(),
                command.category().trim(),
                command.daysOfWeek(),
                command.reminderTime(),
                command.timezone(),
                command.startDate(),
                command.endDate(),
                active
        );
        return routine;
    }

    public List<Routine> list(UUID userId) {
        return routineRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId);
    }

    public RoutineCompletion complete(
            UUID userId,
            UUID routineId,
            LocalDate requestedDate,
            String proofObjectPath,
            String note
    ) {
        Routine routine = ownedRoutine(userId, routineId);
        LocalDate date = requestedDate == null
                ? LocalDate.now(ZoneId.of(routine.getTimezone()))
                : requestedDate;
        if (!routine.isScheduledOn(date)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROUTINE_NOT_SCHEDULED", "해당 날짜에 예정된 루틴이 아닙니다.");
        }
        return completionRepository.findByRoutineIdAndCompletionDate(routineId, date)
                .orElseGet(() -> completionRepository.save(new RoutineCompletion(
                        routineId,
                        userId,
                        date,
                        Instant.now(),
                        normalizeBlank(proofObjectPath),
                        normalizeBlank(note)
                )));
    }

    public List<CalendarDay> calendar(UUID userId, YearMonth month) {
        List<Routine> routines = list(userId);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Map<LocalDate, Long> completedCounts = new HashMap<>();
        completionRepository.findAllByUserIdAndCompletionDateBetweenOrderByCompletionDateAsc(userId, start, end)
                .forEach(completion -> completedCounts.merge(completion.getCompletionDate(), 1L, Long::sum));

        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            long scheduled = routines.stream().filter(routine -> routine.isScheduledOn(currentDate)).count();
            long completed = completedCounts.getOrDefault(currentDate, 0L);
            double rate = scheduled == 0 ? 0 : Math.round((completed * 10000.0) / scheduled) / 100.0;
            days.add(new CalendarDay(currentDate, scheduled, completed, rate));
        }
        return days;
    }

    public Routine ownedRoutine(UUID userId, UUID routineId) {
        return routineRepository.findByIdAndOwnerId(routineId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "루틴을 찾을 수 없습니다."));
    }

    private void validateCommand(RoutineCommand command) {
        if (command.daysOfWeek() == null || command.daysOfWeek().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DAYS_REQUIRED", "실천 요일을 하나 이상 선택해 주세요.");
        }
        if (command.endDate() != null && command.endDate().isBefore(command.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "종료일은 시작일보다 빠를 수 없습니다.");
        }
        try {
            ZoneId.of(command.timezone());
        } catch (ZoneRulesException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "지원하지 않는 시간대입니다.");
        }
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record RoutineCommand(
            String title,
            String category,
            Set<DayOfWeek> daysOfWeek,
            java.time.LocalTime reminderTime,
            String timezone,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record CalendarDay(LocalDate date, long scheduledCount, long completedCount, double completionRate) {
    }
}
