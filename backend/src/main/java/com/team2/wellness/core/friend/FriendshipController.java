package com.team2.wellness.core.friend;

import com.team2.wellness.common.security.CurrentUser;
import com.team2.wellness.core.profile.AvatarStoragePort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendshipController {

    private final CurrentUser currentUser;
    private final FriendshipService friendshipService;
    private final AvatarStoragePort avatarStorage;

    public FriendshipController(
            CurrentUser currentUser,
            FriendshipService friendshipService,
            AvatarStoragePort avatarStorage
    ) {
        this.currentUser = currentUser;
        this.friendshipService = friendshipService;
        this.avatarStorage = avatarStorage;
    }

    @GetMapping
    List<FriendResponse> friends(Authentication authentication) {
        return friendshipService.friends(currentUser.id(authentication)).stream()
                .map(friend -> new FriendResponse(friend.userId(), friend.nickname(), avatarUrl(friend.avatarObjectPath())))
                .toList();
    }

    private String avatarUrl(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) return null;
        try {
            return avatarStorage.temporaryDownloadUrl(objectPath);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @PostMapping("/invites")
    @ResponseStatus(HttpStatus.CREATED)
    InviteResponse createInvite(Authentication authentication) {
        FriendInvite invite = friendshipService.createInvite(currentUser.id(authentication));
        return new InviteResponse(invite.getToken(), invite.getExpiresAt());
    }

    @PostMapping("/invites/{token}/accept")
    FriendshipResponse accept(Authentication authentication, @PathVariable String token) {
        Friendship friendship = friendshipService.acceptInvite(currentUser.id(authentication), token);
        return FriendshipResponse.from(friendship);
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(Authentication authentication, @PathVariable UUID friendId) {
        friendshipService.remove(currentUser.id(authentication), friendId);
    }

    record InviteResponse(String token, Instant expiresAt) {
    }

    record FriendResponse(UUID userId, String nickname, String avatarUrl) {
    }

    record FriendshipResponse(UUID id, UUID firstUserId, UUID secondUserId, String status) {
        static FriendshipResponse from(Friendship friendship) {
            return new FriendshipResponse(
                    friendship.getId(),
                    friendship.getFirstUserId(),
                    friendship.getSecondUserId(),
                    friendship.getStatus().name()
            );
        }
    }
}
