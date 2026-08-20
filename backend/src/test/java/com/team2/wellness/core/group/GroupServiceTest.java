package com.team2.wellness.core.group;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.friend.FriendshipService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupServiceTest {

    private WellnessGroupRepository groupRepository;
    private GroupMemberRepository memberRepository;
    private GroupService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(WellnessGroupRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        service = new GroupService(groupRepository, memberRepository, mock(FriendshipService.class));
    }

    @Test
    void allowsMemberToRemoveThemself() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        WellnessGroup group = new WellnessGroup(ownerId, "Wellness", (short) 8);
        GroupMember member = new GroupMember(group.getId(), memberId, GroupMember.Role.MEMBER);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndUserId(group.getId(), memberId)).thenReturn(Optional.of(member));

        service.removeMember(memberId, group.getId(), memberId);

        verify(memberRepository).delete(member);
    }

    @Test
    void preventsMemberFromRemovingAnotherMember() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        WellnessGroup group = new WellnessGroup(ownerId, "Wellness", (short) 8);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.removeMember(requesterId, group.getId(), targetId))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("GROUP_OWNER_REQUIRED");
    }
}
