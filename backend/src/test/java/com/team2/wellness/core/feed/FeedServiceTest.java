package com.team2.wellness.core.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupMemberRepository;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.profile.AvatarStoragePort;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedServiceTest {

    private RoutineCompletionRepository completions;
    private FeedService service;

    @BeforeEach
    void setUp() {
        FriendshipService friendships = mock(FriendshipService.class);
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        completions = mock(RoutineCompletionRepository.class);
        when(friendships.friends(any())).thenReturn(List.of());
        when(members.findAllByUserId(any())).thenReturn(List.of());
        service = new FeedService(
                friendships,
                members,
                completions,
                mock(RoutineRepository.class),
                mock(ProfileRepository.class),
                mock(RoutineCompletionReactionRepository.class),
                mock(AvatarStoragePort.class)
        );
    }

    @Test
    void firstPageDoesNotBindNullCursor() {
        UUID userId = UUID.randomUUID();
        when(completions.findAllByUserIdInOrderByCompletedAtDesc(any(), any())).thenReturn(List.of());

        assertThat(service.feed(userId, null, 30).items()).isEmpty();

        verify(completions).findAllByUserIdInOrderByCompletedAtDesc(eq(Set.of(userId)), any());
        verify(completions, never()).findFeedBefore(any(), any(), any());
    }

    @Test
    void cursorPageUsesTypedCursorQuery() {
        UUID userId = UUID.randomUUID();
        Instant cursor = Instant.now();
        when(completions.findFeedBefore(any(), eq(cursor), any())).thenReturn(List.of());

        assertThat(service.feed(userId, cursor.toString(), 30).items()).isEmpty();

        verify(completions).findFeedBefore(eq(Set.of(userId)), eq(cursor), any());
        verify(completions, never()).findAllByUserIdInOrderByCompletedAtDesc(any(), any());
    }
}
