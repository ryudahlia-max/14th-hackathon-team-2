package com.team2.wellness.core.feed;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupMemberRepository;
import com.team2.wellness.core.profile.AvatarStoragePort;
import com.team2.wellness.core.profile.Profile;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.RoutineCompletion;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class FeedReactionService {

    private static final Set<String> ALLOWED_TYPES = Set.of("HEART", "SAD", "THUMBS_UP", "FIRE", "SMILE");
    private final RoutineCompletionReactionRepository reactions;
    private final RoutineCompletionRepository completions;
    private final RoutineRepository routines;
    private final ProfileRepository profiles;
    private final FriendshipService friendships;
    private final GroupMemberRepository groupMembers;
    private final AvatarStoragePort avatarStorage;

    public FeedReactionService(
            RoutineCompletionReactionRepository reactions,
            RoutineCompletionRepository completions,
            RoutineRepository routines,
            ProfileRepository profiles,
            FriendshipService friendships,
            GroupMemberRepository groupMembers,
            AvatarStoragePort avatarStorage
    ) {
        this.reactions = reactions;
        this.completions = completions;
        this.routines = routines;
        this.profiles = profiles;
        this.friendships = friendships;
        this.groupMembers = groupMembers;
        this.avatarStorage = avatarStorage;
    }

    public ReactionView react(UUID userId, UUID completionId, String rawType) {
        String type = normalizeType(rawType);
        RoutineCompletion completion = visibleFriendCompletion(userId, completionId);
        RoutineCompletionReaction reaction = reactions.findByCompletionIdAndReactorId(completionId, userId)
                .map(existing -> { existing.changeType(type); return existing; })
                .orElseGet(() -> reactions.save(new RoutineCompletionReaction(
                        completionId,
                        completion.getUserId(),
                        userId,
                        type
                )));
        return ReactionView.from(reaction);
    }

    public void remove(UUID userId, UUID completionId) {
        reactions.findByCompletionIdAndReactorId(completionId, userId).ifPresent(reactions::delete);
    }

    public List<ReceivedReactionView> received(UUID userId) {
        return reactions.findAllByRoutineOwnerIdOrderByCreatedAtDesc(userId).stream().map(reaction -> {
            RoutineCompletion completion = completions.findById(reaction.getCompletionId()).orElse(null);
            String routineTitle = completion == null ? "삭제된 루틴" : routines.findById(completion.getRoutineId())
                    .map(routine -> routine.getTitle()).orElse("삭제된 루틴");
            Profile reactor = profiles.findById(reaction.getReactorId()).orElse(null);
            String reactorNickname = reactor == null ? "알 수 없는 사용자" : reactor.getNickname();
            return new ReceivedReactionView(
                    reaction.getCompletionId(),
                    reaction.getReactorId(),
                    reactorNickname,
                    avatarUrl(reactor),
                    routineTitle,
                    reaction.getType(),
                    reaction.getCreatedAt()
            );
        }).toList();
    }

    private RoutineCompletion visibleFriendCompletion(UUID userId, UUID completionId) {
        RoutineCompletion completion = completions.findById(completionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPLETION_NOT_FOUND", "완료 기록을 찾을 수 없습니다."));
        boolean connected = friendships.areFriends(userId, completion.getUserId())
                || groupMembers.existsSharedGroup(userId, completion.getUserId());
        if (completion.getUserId().equals(userId) || !connected) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REACTION_NOT_ALLOWED", "연결된 사용자의 완료 기록에만 반응할 수 있습니다.");
        }
        return completion;
    }

    private String avatarUrl(Profile profile) {
        if (profile == null || profile.getAvatarObjectPath() == null) return null;
        try {
            return avatarStorage.temporaryDownloadUrl(profile.getAvatarObjectPath());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String normalizeType(String rawType) {
        String type = rawType == null ? "" : rawType.trim().toUpperCase();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REACTION_TYPE", "지원하지 않는 반응입니다.");
        }
        return type;
    }

    public record ReactionView(UUID id, UUID completionId, String type, Instant createdAt) {
        static ReactionView from(RoutineCompletionReaction reaction) {
            return new ReactionView(reaction.getId(), reaction.getCompletionId(), reaction.getType(), reaction.getCreatedAt());
        }
    }

    public record ReceivedReactionView(
            UUID completionId,
            UUID reactorId,
            String reactorNickname,
            String reactorAvatarUrl,
            String routineTitle,
            String type,
            Instant createdAt
    ) {
    }
}
