package com.team2.wellness.core.friend;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.profile.Profile;
import com.team2.wellness.core.profile.ProfileRepository;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class FriendshipService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final FriendInviteRepository inviteRepository;
    private final FriendshipRepository friendshipRepository;
    private final ProfileRepository profileRepository;

    public FriendshipService(
            FriendInviteRepository inviteRepository,
            FriendshipRepository friendshipRepository,
            ProfileRepository profileRepository
    ) {
        this.inviteRepository = inviteRepository;
        this.friendshipRepository = friendshipRepository;
        this.profileRepository = profileRepository;
    }

    public FriendInvite createInvite(UUID inviterId) {
        requireProfile(inviterId);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return inviteRepository.save(new FriendInvite(inviterId, token, Instant.now().plus(INVITE_TTL)));
    }

    public Friendship acceptInvite(UUID acceptingUserId, String token) {
        requireProfile(acceptingUserId);
        FriendInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "초대를 찾을 수 없습니다."));
        if (invite.getInviterId().equals(acceptingUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_INVITE", "본인의 초대는 수락할 수 없습니다.");
        }
        if (invite.getStatus() != FriendInvite.Status.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "INVITE_NOT_PENDING", "이미 처리된 초대입니다.");
        }
        Instant now = Instant.now();
        if (!invite.getExpiresAt().isAfter(now)) {
            invite.expire();
            throw new ApiException(HttpStatus.GONE, "INVITE_EXPIRED", "만료된 초대입니다.");
        }

        UUID first = Friendship.normalizeFirst(invite.getInviterId(), acceptingUserId);
        UUID second = first.equals(invite.getInviterId()) ? acceptingUserId : invite.getInviterId();
        Friendship friendship = friendshipRepository.findByFirstUserIdAndSecondUserId(first, second)
                .map(existing -> {
                    existing.accept();
                    return existing;
                })
                .orElseGet(() -> friendshipRepository.save(new Friendship(invite.getInviterId(), acceptingUserId)));
        invite.accept(acceptingUserId, now);
        return friendship;
    }

    public List<FriendSummary> friends(UUID userId) {
        List<Friendship> friendships = friendshipRepository.findAllForUser(userId, Friendship.Status.ACCEPTED);
        List<UUID> ids = friendships.stream().map(friendship -> friendship.otherUser(userId)).toList();
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(ids).forEach(profile -> profiles.put(profile.getId(), profile));
        return ids.stream().map(id -> {
            Profile profile = profiles.get(id);
            return new FriendSummary(
                    id,
                    profile == null ? "알 수 없는 사용자" : profile.getNickname(),
                    profile == null ? null : profile.getAvatarObjectPath()
            );
        }).toList();
    }

    public void remove(UUID userId, UUID friendId) {
        Friendship friendship = findPair(userId, friendId);
        friendship.remove();
    }

    public boolean areFriends(UUID userId, UUID otherUserId) {
        UUID first = Friendship.normalizeFirst(userId, otherUserId);
        UUID second = first.equals(userId) ? otherUserId : userId;
        return friendshipRepository.findByFirstUserIdAndSecondUserId(first, second)
                .map(friendship -> friendship.getStatus() == Friendship.Status.ACCEPTED)
                .orElse(false);
    }

    private Friendship findPair(UUID userId, UUID friendId) {
        UUID first = Friendship.normalizeFirst(userId, friendId);
        UUID second = first.equals(userId) ? friendId : userId;
        return friendshipRepository.findByFirstUserIdAndSecondUserId(first, second)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FRIENDSHIP_NOT_FOUND", "친구 관계가 없습니다."));
    }

    private void requireProfile(UUID userId) {
        if (!profileRepository.existsById(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PROFILE_REQUIRED", "프로필 설정이 필요합니다.");
        }
    }

    public record FriendSummary(UUID userId, String nickname, String avatarObjectPath) {
    }
}
