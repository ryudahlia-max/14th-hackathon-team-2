package com.team2.wellness.engagement.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoreAccessPort {
    boolean areAcceptedFriends(UUID userId, UUID targetUserId);
    boolean isGroupMember(UUID userId, UUID groupId);
    List<UUID> getGroupMemberIds(UUID groupId);
    Optional<MissedRoutineOccurrence> getMissedRoutineOccurrence(UUID occurrenceId, UUID targetUserId);
    Optional<UserSummary> getUserSummary(UUID userId);
    Optional<GroupSummary> getGroupSummary(UUID groupId);
    boolean hasAiImageConsent(UUID userId);

    record MissedRoutineOccurrence(UUID occurrenceId, UUID routineId, UUID targetUserId) { }
    record UserSummary(UUID userId, String nickname) { }
    record GroupSummary(UUID groupId, String name) { }
}
