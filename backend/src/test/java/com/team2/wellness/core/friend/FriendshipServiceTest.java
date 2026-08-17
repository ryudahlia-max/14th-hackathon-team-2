package com.team2.wellness.core.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.profile.ProfileRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FriendshipServiceTest {

    private FriendInviteRepository inviteRepository;
    private FriendshipRepository friendshipRepository;
    private ProfileRepository profileRepository;
    private FriendshipService service;

    @BeforeEach
    void setUp() {
        inviteRepository = mock(FriendInviteRepository.class);
        friendshipRepository = mock(FriendshipRepository.class);
        profileRepository = mock(ProfileRepository.class);
        service = new FriendshipService(inviteRepository, friendshipRepository, profileRepository);
    }

    @Test
    void acceptsInviteAndCreatesNormalizedFriendship() {
        UUID inviter = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID accepting = UUID.fromString("00000000-0000-0000-0000-000000000001");
        FriendInvite invite = new FriendInvite(inviter, "invite-token", Instant.now().plusSeconds(3600));

        when(profileRepository.existsById(accepting)).thenReturn(true);
        when(inviteRepository.findByToken("invite-token")).thenReturn(Optional.of(invite));
        when(friendshipRepository.findByFirstUserIdAndSecondUserId(accepting, inviter))
                .thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Friendship friendship = service.acceptInvite(accepting, "invite-token");

        assertThat(friendship.getFirstUserId()).isEqualTo(accepting);
        assertThat(friendship.getSecondUserId()).isEqualTo(inviter);
        assertThat(friendship.getStatus()).isEqualTo(Friendship.Status.ACCEPTED);
        assertThat(invite.getStatus()).isEqualTo(FriendInvite.Status.ACCEPTED);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    void rejectsOwnInvite() {
        UUID userId = UUID.randomUUID();
        FriendInvite invite = new FriendInvite(userId, "own-token", Instant.now().plusSeconds(3600));
        when(profileRepository.existsById(userId)).thenReturn(true);
        when(inviteRepository.findByToken("own-token")).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.acceptInvite(userId, "own-token"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("SELF_INVITE");
    }

    @Test
    void reportsAcceptedFriendshipOnly() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(friendshipRepository.findByFirstUserIdAndSecondUserId(first, second))
                .thenReturn(Optional.of(new Friendship(second, first)));

        assertThat(service.areFriends(first, second)).isTrue();
    }
}
