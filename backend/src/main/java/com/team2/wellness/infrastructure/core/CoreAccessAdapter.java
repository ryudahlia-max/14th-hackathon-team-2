package com.team2.wellness.infrastructure.core;

import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupService;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CoreAccessAdapter implements CoreAccessPort {

    private static final int MAX_MISSED_LOOKBACK_DAYS = 366;

    private final FriendshipService friendshipService;
    private final GroupService groupService;
    private final ProfileRepository profileRepository;
    private final RoutineRepository routineRepository;
    private final RoutineCompletionRepository completionRepository;

    public CoreAccessAdapter(
            FriendshipService friendshipService,
            GroupService groupService,
            ProfileRepository profileRepository,
            RoutineRepository routineRepository,
            RoutineCompletionRepository completionRepository
    ) {
        this.friendshipService = friendshipService;
        this.groupService = groupService;
        this.profileRepository = profileRepository;
        this.routineRepository = routineRepository;
        this.completionRepository = completionRepository;
    }

    @Override
    public boolean areAcceptedFriends(UUID userId, UUID targetUserId) {
        return friendshipService.areFriends(userId, targetUserId);
    }

    @Override
    public boolean isGroupMember(UUID userId, UUID groupId) {
        return groupService.isMember(groupId, userId);
    }

    @Override
    public List<UUID> getGroupMemberIds(UUID groupId) {
        return groupService.memberIds(groupId);
    }

    @Override
    public Optional<MissedRoutineOccurrence> getMissedRoutineOccurrence(UUID occurrenceId, UUID targetUserId) {
        return routineRepository.findByIdAndOwnerId(occurrenceId, targetUserId)
                .flatMap(routine -> mostRecentMissedDate(routine)
                        .map(date -> new MissedRoutineOccurrence(occurrenceId, routine.getId(), targetUserId)));
    }

    @Override
    public Optional<UserSummary> getUserSummary(UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> new UserSummary(profile.getId(), profile.getNickname()));
    }

    @Override
    public boolean hasAiImageConsent(UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> profile.isAiFaceConsent() && profile.getAvatarObjectPath() != null)
                .orElse(false);
    }

    private Optional<LocalDate> mostRecentMissedDate(Routine routine) {
        LocalDate today = LocalDate.now(ZoneId.of(routine.getTimezone()));
        LocalDate earliest = today.minusDays(MAX_MISSED_LOOKBACK_DAYS);
        if (routine.getStartDate().isAfter(earliest)) {
            earliest = routine.getStartDate();
        }

        for (LocalDate date = today.minusDays(1); !date.isBefore(earliest); date = date.minusDays(1)) {
            if (routine.isScheduledOn(date)
                    && completionRepository.findByRoutineIdAndCompletionDate(routine.getId(), date).isEmpty()) {
                return Optional.of(date);
            }
        }
        return Optional.empty();
    }
}
