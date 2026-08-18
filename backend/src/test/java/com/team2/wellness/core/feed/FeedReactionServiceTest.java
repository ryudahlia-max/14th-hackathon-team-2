package com.team2.wellness.core.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.RoutineCompletion;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeedReactionServiceTest {

    private final RoutineCompletionReactionRepository reactions = mock(RoutineCompletionReactionRepository.class);
    private final RoutineCompletionRepository completions = mock(RoutineCompletionRepository.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final FriendshipService friendships = mock(FriendshipService.class);
    private FeedReactionService service;

    @BeforeEach
    void setUp() {
        service = new FeedReactionService(reactions, completions, routines, profiles, friendships);
    }

    @Test
    void friendCanReactAndCamelCaseTypeIsNormalized() {
        UUID reactor = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        RoutineCompletion completion = new RoutineCompletion(
                UUID.randomUUID(), owner, LocalDate.of(2026, 8, 18), java.time.Instant.now(), null, null
        );
        when(completions.findById(completion.getId())).thenReturn(Optional.of(completion));
        when(friendships.areFriends(reactor, owner)).thenReturn(true);
        when(reactions.findByCompletionIdAndReactorId(completion.getId(), reactor)).thenReturn(Optional.empty());
        when(reactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeedReactionService.ReactionView result = service.react(reactor, completion.getId(), "THUMBS_UP");

        assertThat(result.type()).isEqualTo("THUMBS_UP");
    }

    @Test
    void nonFriendCannotReact() {
        UUID reactor = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        RoutineCompletion completion = new RoutineCompletion(
                UUID.randomUUID(), owner, LocalDate.of(2026, 8, 18), java.time.Instant.now(), null, null
        );
        when(completions.findById(completion.getId())).thenReturn(Optional.of(completion));

        assertThatThrownBy(() -> service.react(reactor, completion.getId(), "HEART"))
                .isInstanceOf(ApiException.class);
        verify(reactions, never()).save(any());
    }
}
