package com.team2.wellness.infrastructure.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team2.wellness.core.friend.FriendshipService;
import com.team2.wellness.core.group.GroupService;
import com.team2.wellness.core.profile.Profile;
import com.team2.wellness.core.profile.ProfileRepository;
import com.team2.wellness.core.routine.RoutineService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreAccessAdapterTest {

    private FriendshipService friendships;
    private GroupService groups;
    private ProfileRepository profiles;
    private RoutineService routines;
    private CoreAccessAdapter adapter;

    @BeforeEach
    void setUp() {
        friendships = mock(FriendshipService.class);
        groups = mock(GroupService.class);
        profiles = mock(ProfileRepository.class);
        routines = mock(RoutineService.class);
        adapter = new CoreAccessAdapter(friendships, groups, profiles, routines);
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
        UUID routineId = UUID.randomUUID();
        when(routines.missedRoutine(targetId, routineId)).thenReturn(Optional.of(new RoutineService.MissedRoutine(
                routineId,
                "매일 걷기",
                "운동",
                LocalDate.now().minusDays(1),
                4
        )));

        assertThat(adapter.getMissedRoutineOccurrence(routineId, targetId))
                .hasValueSatisfying(occurrence -> {
                    assertThat(occurrence.occurrenceId()).isEqualTo(routineId);
                    assertThat(occurrence.targetUserId()).isEqualTo(targetId);
                    assertThat(occurrence.routineTitle()).isEqualTo("매일 걷기");
                    assertThat(occurrence.routineCategory()).isEqualTo("운동");
                    assertThat(occurrence.missedCount()).isEqualTo(4);
                });
    }
}
