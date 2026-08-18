package com.team2.wellness.infrastructure.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupService;
import com.team2.wellness.core.profile.Profile;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.Routine;
import com.team2.wellness.core.routine.RoutineCompletionRepository;
import com.team2.wellness.core.routine.RoutineRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreAccessAdapterTest {

    private FriendshipService friendships;
    private GroupService groups;
    private ProfileRepository profiles;
    private RoutineRepository routines;
    private RoutineCompletionRepository completions;
    private CoreAccessAdapter adapter;

    @BeforeEach
    void setUp() {
        friendships = mock(FriendshipService.class);
        groups = mock(GroupService.class);
        profiles = mock(ProfileRepository.class);
        routines = mock(RoutineRepository.class);
        completions = mock(RoutineCompletionRepository.class);
        adapter = new CoreAccessAdapter(friendships, groups, profiles, routines, completions);
    }

    @Test
    void consentRequiresBothOptInAndRegisteredFaceAsset() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile(userId, "사용자", "Asia/Seoul");
        profile.update("사용자", "Asia/Seoul", "avatars/face.png", true);
        when(profiles.findById(userId)).thenReturn(Optional.of(profile));

        assertThat(adapter.hasAiImageConsent(userId)).isTrue();

        profile.update("사용자", "Asia/Seoul", null, true);
        assertThat(adapter.hasAiImageConsent(userId)).isFalse();
    }

    @Test
    void routineIdRepresentsAnOccurrenceOnlyWhenAScheduledDayWasMissed() {
        UUID targetId = UUID.randomUUID();
        Routine routine = new Routine(
                targetId,
                "매일 걷기",
                "운동",
                EnumSet.allOf(DayOfWeek.class),
                LocalTime.of(20, 0),
                "Asia/Seoul",
                LocalDate.now().minusDays(10),
                null
        );
        when(routines.findByIdAndOwnerId(routine.getId(), targetId)).thenReturn(Optional.of(routine));
        when(completions.findByRoutineIdAndCompletionDate(routine.getId(), LocalDate.now().minusDays(1)))
                .thenReturn(Optional.empty());

        assertThat(adapter.getMissedRoutineOccurrence(routine.getId(), targetId))
                .hasValueSatisfying(occurrence -> {
                    assertThat(occurrence.occurrenceId()).isEqualTo(routine.getId());
                    assertThat(occurrence.targetUserId()).isEqualTo(targetId);
                });
    }
}
