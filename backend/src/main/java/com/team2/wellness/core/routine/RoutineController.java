package com.team2.wellness.core.routine;

import com.team2.wellness.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final CurrentUser currentUser;
    private final RoutineService routineService;
    private final com.team2.wellness.core.friend.FriendshipService friendshipService;

    public RoutineController(
            CurrentUser currentUser,
            RoutineService routineService,
            com.team2.wellness.core.friend.FriendshipService friendshipService
    ) {
        this.currentUser = currentUser;
        this.routineService = routineService;
        this.friendshipService = friendshipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RoutineResponse create(Authentication authentication, @Valid @RequestBody RoutineRequest request) {
        return RoutineResponse.from(routineService.create(currentUser.id(authentication), request.toCommand()));
    }

    @GetMapping
    List<RoutineResponse> list(Authentication authentication) {
        return routineService.list(currentUser.id(authentication)).stream().map(RoutineResponse::from).toList();
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication authentication, @PathVariable UUID routineId) {
        routineService.delete(currentUser.id(authentication), routineId);
    }

    @PatchMapping("/{routineId}")
    RoutineResponse update(
            Authentication authentication,
            @PathVariable UUID routineId,
            @Valid @RequestBody UpdateRoutineRequest request
    ) {
        return RoutineResponse.from(routineService.update(
                currentUser.id(authentication),
                routineId,
                request.routine().toCommand(),
                request.active()
        ));
    }

    @PostMapping("/{routineId}/completions")
    RoutineCompletionResponse complete(
            Authentication authentication,
            @PathVariable UUID routineId,
            @Valid @RequestBody CompletionRequest request
    ) {
        return RoutineCompletionResponse.from(routineService.complete(
                currentUser.id(authentication),
                routineId,
                request.completionDate(),
                request.proofObjectPath(),
                request.note()
        ));
    }

    @DeleteMapping("/{routineId}/completions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void uncomplete(
            Authentication authentication,
            @PathVariable UUID routineId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        routineService.uncomplete(currentUser.id(authentication), routineId, date);
    }

    @GetMapping("/calendar")
    List<RoutineService.CalendarDay> calendar(
            Authentication authentication,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return routineService.calendar(currentUser.id(authentication), month);
    }

    @GetMapping("/friends/{friendId}/missed")
    List<RoutineService.MissedRoutine> missed(
            Authentication authentication,
            @PathVariable UUID friendId
    ) {
        UUID requesterId = currentUser.id(authentication);
        if (!friendshipService.areFriends(requesterId, friendId)) {
            throw new com.team2.wellness.common.api.ApiException(
                    HttpStatus.FORBIDDEN,
                    "FRIENDSHIP_REQUIRED",
                    "친구의 미완료 루틴만 확인할 수 있습니다."
            );
        }
        return routineService.missedRoutines(friendId);
    }

    @GetMapping("/friends/{friendId}")
    List<RoutineResponse> friendRoutines(
            Authentication authentication,
            @PathVariable UUID friendId
    ) {
        requireFriend(currentUser.id(authentication), friendId);
        return routineService.list(friendId).stream().map(RoutineResponse::from).toList();
    }

    @GetMapping("/friends/{friendId}/calendar")
    List<RoutineService.CalendarDay> friendCalendar(
            Authentication authentication,
            @PathVariable UUID friendId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        requireFriend(currentUser.id(authentication), friendId);
        return routineService.calendar(friendId, month);
    }

    private void requireFriend(UUID requesterId, UUID friendId) {
        if (!friendshipService.areFriends(requesterId, friendId)) {
            throw new com.team2.wellness.common.api.ApiException(
                    HttpStatus.FORBIDDEN,
                    "FRIENDSHIP_REQUIRED",
                    "친구의 루틴만 확인할 수 있습니다."
            );
        }
    }

    record RoutineRequest(
            @NotBlank @Size(max = 80) String title,
            @NotBlank @Size(max = 30) String category,
            @jakarta.validation.constraints.Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
            @NotEmpty Set<DayOfWeek> daysOfWeek,
            @NotNull LocalTime reminderTime,
            @NotNull LocalTime completionDeadline,
            @NotBlank String timezone,
            @NotNull LocalDate startDate,
            LocalDate endDate
    ) {
        RoutineService.RoutineCommand toCommand() {
            return new RoutineService.RoutineCommand(
                    title,
                    category,
                    color,
                    daysOfWeek,
                    reminderTime,
                    completionDeadline,
                    timezone,
                    startDate,
                    endDate
            );
        }
    }

    record UpdateRoutineRequest(@Valid @NotNull RoutineRequest routine, boolean active) {
    }

    record CompletionRequest(
            LocalDate completionDate,
            @Size(max = 500) String proofObjectPath,
            @Size(max = 500) String note
    ) {
    }

    record RoutineResponse(
            UUID id,
            String title,
            String category,
            String color,
            Set<DayOfWeek> daysOfWeek,
            LocalTime reminderTime,
            LocalTime completionDeadline,
            String timezone,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        static RoutineResponse from(Routine routine) {
            return new RoutineResponse(
                    routine.getId(),
                    routine.getTitle(),
                    routine.getCategory(),
                    routine.getColor(),
                    routine.getDaysOfWeek(),
                    routine.getReminderTime(),
                    routine.getCompletionDeadline(),
                    routine.getTimezone(),
                    routine.getStartDate(),
                    routine.getEndDate(),
                    routine.isActive()
            );
        }
    }

    record RoutineCompletionResponse(
            UUID id,
            UUID routineId,
            LocalDate completionDate,
            Instant completedAt,
            String proofObjectPath,
            String note
    ) {
        static RoutineCompletionResponse from(RoutineCompletion completion) {
            return new RoutineCompletionResponse(
                    completion.getId(),
                    completion.getRoutineId(),
                    completion.getCompletionDate(),
                    completion.getCompletedAt(),
                    completion.getProofObjectPath(),
                    completion.getNote()
            );
        }
    }
}
