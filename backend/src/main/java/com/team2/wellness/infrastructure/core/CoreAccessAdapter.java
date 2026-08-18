package com.team2.wellness.infrastructure.core;

import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupService;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.RoutineService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CoreAccessAdapter implements CoreAccessPort {

    private final FriendshipService friendshipService;
    private final GroupService groupService;
    private final ProfileRepository profileRepository;
    private final RoutineService routineService;

    public CoreAccessAdapter(
            FriendshipService friendshipService,
            GroupService groupService,
            ProfileRepository profileRepository,
            RoutineService routineService
    ) {
        this.friendshipService = friendshipService;
        this.groupService = groupService;
        this.profileRepository = profileRepository;
        this.routineService = routineService;
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
        return routineService.missedRoutine(targetUserId, occurrenceId)
                .map(routine -> new MissedRoutineOccurrence(
                        occurrenceId,
                        routine.routineId(),
                        targetUserId,
                        routine.title(),
                        routine.category(),
                        routine.missedCount(),
                        routine.missedDate()
                ));
    }

    @Override
    public Optional<UserSummary> getUserSummary(UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> new UserSummary(profile.getId(), profile.getNickname()));
    }

    @Override
    public Optional<GroupSummary> getGroupSummary(UUID groupId) {
        try {
            return Optional.of(new GroupSummary(groupId, groupService.name(groupId)));
        } catch (com.team2.wellness.common.api.ApiException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasAiImageConsent(UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> profile.isAiFaceConsent() && profile.getAvatarObjectPath() != null)
                .orElse(false);
    }
}
