package com.team2.wellness.core.friend;

import com.team2.wellness.common.security.CurrentUser;
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

    public FriendshipController(CurrentUser currentUser, FriendshipService friendshipService) {
        this.currentUser = currentUser;
        this.friendshipService = friendshipService;
    }

    @GetMapping
    List<FriendshipService.FriendSummary> friends(Authentication authentication) {
        return friendshipService.friends(currentUser.id(authentication));
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
