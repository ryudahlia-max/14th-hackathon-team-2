package com.team2.wellness.core.group;

import com.team2.wellness.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final CurrentUser currentUser;
    private final GroupService groupService;

    public GroupController(CurrentUser currentUser, GroupService groupService) {
        this.currentUser = currentUser;
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GroupService.GroupDetail create(Authentication authentication, @Valid @RequestBody CreateGroupRequest request) {
        UUID userId = currentUser.id(authentication);
        WellnessGroup group = groupService.create(userId, request.name(), request.memberIds());
        return groupService.detail(userId, group.getId());
    }

    @GetMapping
    List<GroupService.GroupSummary> list(Authentication authentication) {
        return groupService.list(currentUser.id(authentication));
    }

    @GetMapping("/{groupId}")
    GroupService.GroupDetail detail(Authentication authentication, @PathVariable UUID groupId) {
        return groupService.detail(currentUser.id(authentication), groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addMember(
            Authentication authentication,
            @PathVariable UUID groupId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        groupService.addMember(currentUser.id(authentication), groupId, request.userId());
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(Authentication authentication, @PathVariable UUID groupId, @PathVariable UUID userId) {
        groupService.removeMember(currentUser.id(authentication), groupId, userId);
    }

    record CreateGroupRequest(
            @NotBlank @Size(max = 50) String name,
            @NotNull @Size(max = 7) List<UUID> memberIds
    ) {
    }

    record AddMemberRequest(@NotNull UUID userId) {
    }
}
