package com.team2.wellness.core.group;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.core.friend.FriendshipService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class GroupService {

    private static final short DEFAULT_MAX_MEMBERS = 8;

    private final WellnessGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final FriendshipService friendshipService;

    public GroupService(
            WellnessGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            FriendshipService friendshipService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.friendshipService = friendshipService;
    }

    public WellnessGroup create(UUID ownerId, String name, List<UUID> initialMemberIds) {
        WellnessGroup group = groupRepository.save(new WellnessGroup(ownerId, name.trim(), DEFAULT_MAX_MEMBERS));
        memberRepository.save(new GroupMember(group.getId(), ownerId, GroupMember.Role.OWNER));
        initialMemberIds.stream().distinct().filter(userId -> !userId.equals(ownerId)).forEach(userId -> {
            if (!friendshipService.areFriends(ownerId, userId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_ACCEPTED_FRIEND", "친구만 그룹에 초대할 수 있습니다.");
            }
            addMemberInternal(group, userId);
        });
        return group;
    }

    public List<GroupSummary> list(UUID userId) {
        List<UUID> groupIds = memberRepository.findAllByUserId(userId).stream()
                .map(GroupMember::getGroupId)
                .toList();
        return groupRepository.findAllById(groupIds).stream()
                .map(group -> new GroupSummary(
                        group.getId(),
                        group.getName(),
                        group.getOwnerId(),
                        memberRepository.countByGroupId(group.getId()),
                        group.getMaxMembers()
                ))
                .toList();
    }

    public GroupDetail detail(UUID userId, UUID groupId) {
        requireMember(groupId, userId);
        WellnessGroup group = findGroup(groupId);
        List<GroupMemberView> members = memberRepository.findAllByGroupId(groupId).stream()
                .map(member -> new GroupMemberView(member.getUserId(), member.getRole().name()))
                .toList();
        return new GroupDetail(group.getId(), group.getName(), group.getOwnerId(), group.getMaxMembers(), members);
    }

    public void addMember(UUID ownerId, UUID groupId, UUID userId) {
        WellnessGroup group = requireOwner(groupId, ownerId);
        if (!friendshipService.areFriends(ownerId, userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_ACCEPTED_FRIEND", "친구만 그룹에 초대할 수 있습니다.");
        }
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            return;
        }
        addMemberInternal(group, userId);
    }

    public void removeMember(UUID requesterId, UUID groupId, UUID userId) {
        WellnessGroup group = findGroup(groupId);
        boolean isOwner = group.getOwnerId().equals(requesterId);
        boolean isSelfRemoval = requesterId.equals(userId);
        if (!isOwner && !isSelfRemoval) {
            throw new ApiException(HttpStatus.FORBIDDEN, "GROUP_OWNER_REQUIRED", "그룹 소유자 권한이 필요합니다.");
        }
        if (group.getOwnerId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWNER_CANNOT_BE_REMOVED", "그룹 소유자는 제거할 수 없습니다.");
        }
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_MEMBER_NOT_FOUND", "그룹 멤버가 아닙니다."));
        memberRepository.delete(member);
    }

    public boolean isMember(UUID groupId, UUID userId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
    }

    public List<UUID> memberIds(UUID groupId) {
        return memberRepository.findAllByGroupId(groupId).stream().map(GroupMember::getUserId).toList();
    }

    public String name(UUID groupId) {
        return findGroup(groupId).getName();
    }

    private void addMemberInternal(WellnessGroup group, UUID userId) {
        if (memberRepository.countByGroupId(group.getId()) >= group.getMaxMembers()) {
            throw new ApiException(HttpStatus.CONFLICT, "GROUP_FULL", "그룹 정원을 초과했습니다.");
        }
        memberRepository.save(new GroupMember(group.getId(), userId, GroupMember.Role.MEMBER));
    }

    private WellnessGroup requireOwner(UUID groupId, UUID userId) {
        WellnessGroup group = findGroup(groupId);
        if (!group.getOwnerId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "GROUP_OWNER_REQUIRED", "그룹 소유자 권한이 필요합니다.");
        }
        return group;
    }

    private void requireMember(UUID groupId, UUID userId) {
        if (!isMember(groupId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "GROUP_ACCESS_DENIED", "그룹 접근 권한이 없습니다.");
        }
    }

    private WellnessGroup findGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다."));
    }

    public record GroupSummary(UUID id, String name, UUID ownerId, long memberCount, short maxMembers) {
    }

    public record GroupMemberView(UUID userId, String role) {
    }

    public record GroupDetail(
            UUID id,
            String name,
            UUID ownerId,
            short maxMembers,
            List<GroupMemberView> members
    ) {
    }
}
