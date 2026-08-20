package com.team2.wellness.core.routine;

import com.team2.wellness.common.api.ApiException;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
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
                normalizeColor(command.color()),
                command.daysOfWeek(),
                command.reminderTime(),
                command.completionDeadline(),
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
                normalizeColor(command.color()),
                command.daysOfWeek(),
                command.reminderTime(),
                command.completionDeadline(),
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

    public void delete(UUID userId, UUID routineId) {
        routineRepository.delete(ownedRoutine(userId, routineId));
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
        Map<LocalDate, Set<UUID>> completedRoutineIds = new HashMap<>();
        completionRepository.findAllByUserIdAndCompletionDateBetweenOrderByCompletionDateAsc(userId, start, end)
                .forEach(completion -> completedRoutineIds
                        .computeIfAbsent(completion.getCompletionDate(), ignored -> new HashSet<>())
                        .add(completion.getRoutineId()));

        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            long scheduled = routines.stream().filter(routine -> routine.isScheduledOn(currentDate)).count();
            Set<UUID> completedIds = completedRoutineIds.getOrDefault(currentDate, Set.of());
            long completed = completedIds.size();
            double rate = scheduled == 0 ? 0 : Math.round((completed * 10000.0) / scheduled) / 100.0;
            days.add(new CalendarDay(currentDate, scheduled, completed, rate, List.copyOf(completedIds)));
        }
        return days;
    }

    public void uncomplete(UUID userId, UUID routineId, LocalDate date) {
        ownedRoutine(userId, routineId);
        completionRepository.findByRoutineIdAndCompletionDate(routineId, date)
                .ifPresent(completionRepository::delete);
    }

    public List<MissedRoutine> missedRoutines(UUID ownerId) {
        Instant now = Instant.now();
        return list(ownerId).stream()
                .map(routine -> missedRoutine(routine, now))
                .flatMap(Optional::stream)
                .toList();
    }

    public Optional<MissedRoutine> missedRoutine(UUID ownerId, UUID routineId) {
        return missedRoutine(ownerId, routineId, Instant.now());
    }

    Optional<MissedRoutine> missedRoutine(UUID ownerId, UUID routineId, Instant now) {
        return routineRepository.findByIdAndOwnerId(routineId, ownerId)
                .flatMap(routine -> missedRoutine(routine, now));
    }

    private Optional<MissedRoutine> missedRoutine(Routine routine, Instant now) {
        ZonedDateTime localNow = now.atZone(ZoneId.of(routine.getTimezone()));
        LocalDate today = localNow.toLocalDate();
        LocalDate earliest = today.minusDays(366);
        if (routine.getStartDate().isAfter(earliest)) {
            earliest = routine.getStartDate();
        }
        LocalDate latest = localNow.toLocalTime().isBefore(routine.getCompletionDeadline())
                ? today.minusDays(1)
                : today;
        if (routine.getEndDate() != null && routine.getEndDate().isBefore(latest)) {
            latest = routine.getEndDate();
        }
        if (latest.isBefore(earliest)) {
            return Optional.empty();
        }

        Set<LocalDate> completedDates = new HashSet<>();
        completionRepository.findAllByRoutineIdAndCompletionDateBetween(routine.getId(), earliest, latest)
                .forEach(completion -> completedDates.add(completion.getCompletionDate()));

        int missedCount = 0;
        LocalDate mostRecentMissedDate = null;
        for (LocalDate date = latest; !date.isBefore(earliest); date = date.minusDays(1)) {
            if (routine.isScheduledOn(date) && !completedDates.contains(date)) {
                missedCount++;
                if (mostRecentMissedDate == null) {
                    mostRecentMissedDate = date;
                }
            }
        }
        if (missedCount == 0) {
            return Optional.empty();
        }
        return Optional.of(new MissedRoutine(
                routine.getId(),
                routine.getTitle(),
                routine.getCategory(),
                mostRecentMissedDate,
                missedCount
        ));
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

    private String normalizeColor(String value) {
        String color = value == null || value.isBlank() ? "#60A5FA" : value.trim().toUpperCase();
        if (!color.matches("^#[0-9A-F]{6}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROUTINE_COLOR", "루틴 색상 형식을 확인해 주세요.");
        }
        return color;
    }

    public record RoutineCommand(
            String title,
            String category,
            String color,
            Set<DayOfWeek> daysOfWeek,
            java.time.LocalTime reminderTime,
            java.time.LocalTime completionDeadline,
            String timezone,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record CalendarDay(
            LocalDate date,
            long scheduledCount,
            long completedCount,
            double completionRate,
            List<UUID> completedRoutineIds
    ) {
    }

    public record MissedRoutine(
            UUID routineId,
            String title,
            String category,
            LocalDate missedDate,
            int missedCount
    ) {
    }
}
