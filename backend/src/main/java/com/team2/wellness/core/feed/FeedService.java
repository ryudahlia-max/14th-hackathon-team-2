package com.team2.wellness.core.feed;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.common.api.CursorPage;
import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupMember;
import com.team2.wellness.core.group.GroupMemberRepository;
import com.team2.wellness.core.profile.Profile;
import com.team2.wellness.core.profile.AvatarStoragePort;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineCompletion;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class FeedService {

    private final FriendshipService friendshipService;
    private final GroupMemberRepository memberRepository;
    private final RoutineCompletionRepository completionRepository;
    private final RoutineRepository routineRepository;
    private final ProfileRepository profileRepository;
    private final RoutineCompletionReactionRepository reactionRepository;
    private final AvatarStoragePort avatarStorage;

    public FeedService(
            FriendshipService friendshipService,
            GroupMemberRepository memberRepository,
            RoutineCompletionRepository completionRepository,
            RoutineRepository routineRepository,
            ProfileRepository profileRepository,
            RoutineCompletionReactionRepository reactionRepository,
            AvatarStoragePort avatarStorage
    ) {
        this.friendshipService = friendshipService;
        this.memberRepository = memberRepository;
        this.completionRepository = completionRepository;
        this.routineRepository = routineRepository;
        this.profileRepository = profileRepository;
        this.reactionRepository = reactionRepository;
        this.avatarStorage = avatarStorage;
    }

    public CursorPage<FeedItem> feed(UUID userId, String cursorValue, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 50));
        Instant cursor = parseCursor(cursorValue);
        Set<UUID> visibleUserIds = visibleUserIds(userId);
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<RoutineCompletion> completions = cursor == null
                ? completionRepository.findAllByUserIdInOrderByCompletedAtDesc(visibleUserIds, pageRequest)
                : completionRepository.findFeedBefore(visibleUserIds, cursor, pageRequest);
        boolean hasNext = completions.size() > limit;
        List<RoutineCompletion> page = hasNext ? completions.subList(0, limit) : completions;

        Map<UUID, Routine> routines = new HashMap<>();
        routineRepository.findAllById(page.stream().map(RoutineCompletion::getRoutineId).toList())
                .forEach(routine -> routines.put(routine.getId(), routine));
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(page.stream().map(RoutineCompletion::getUserId).toList())
                .forEach(profile -> profiles.put(profile.getId(), profile));

        List<FeedItem> items = page.stream().map(completion -> {
            Routine routine = routines.get(completion.getRoutineId());
            Profile profile = profiles.get(completion.getUserId());
            String myReaction = reactionRepository.findByCompletionIdAndReactorId(completion.getId(), userId)
                    .map(RoutineCompletionReaction::getType)
                    .orElse(null);
            return new FeedItem(
                    completion.getId(),
                    completion.getUserId(),
                    profile == null ? "알 수 없는 사용자" : profile.getNickname(),
                    avatarUrl(profile),
                    completion.getRoutineId(),
                    routine == null ? "삭제된 루틴" : routine.getTitle(),
                    completion.getCompletionDate(),
                    completion.getCompletedAt(),
                    completion.getProofObjectPath(),
                    completion.getNote(),
                    myReaction
            );
        }).toList();

        String nextCursor = hasNext && !page.isEmpty()
                ? page.get(page.size() - 1).getCompletedAt().toString()
                : null;
        return new CursorPage<>(items, nextCursor);
    }

    private String avatarUrl(Profile profile) {
        if (profile == null || profile.getAvatarObjectPath() == null) return null;
        try {
            return avatarStorage.temporaryDownloadUrl(profile.getAvatarObjectPath());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Set<UUID> visibleUserIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        ids.add(userId);
        friendshipService.friends(userId).forEach(friend -> ids.add(friend.userId()));
        List<UUID> groupIds = memberRepository.findAllByUserId(userId).stream()
                .map(GroupMember::getGroupId)
                .toList();
        groupIds.forEach(groupId -> memberRepository.findAllByGroupId(groupId)
                .forEach(member -> ids.add(member.getUserId())));
        return ids;
    }

    private Instant parseCursor(String cursorValue) {
        if (cursorValue == null || cursorValue.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(cursorValue);
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "잘못된 페이지 커서입니다.");
        }
    }

    public record FeedItem(
            UUID completionId,
            UUID userId,
            String nickname,
            String avatarUrl,
            UUID routineId,
            String routineTitle,
            java.time.LocalDate completionDate,
            Instant completedAt,
            String proofObjectPath,
            String note,
            String myReaction
    ) {
    }
}
