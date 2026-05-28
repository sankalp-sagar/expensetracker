package com.sankalp.expensetracker.group.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.group.dto.CreateGroupRequest;
import com.sankalp.expensetracker.group.dto.GroupResponse;
import com.sankalp.expensetracker.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group creation, invites, membership")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @Operation(summary = "Create a new expense group")
    public ResponseEntity<ApiResponse<GroupResponse>> create(@RequestHeader("X-User-Id") UUID userId,
                                                             @Valid @RequestBody CreateGroupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(groupService.createGroup(userId, req), "Group created"));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group details (members only)")
    public ResponseEntity<ApiResponse<GroupResponse>> get(@RequestHeader("X-User-Id") UUID userId,
                                                          @PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.ok(groupService.get(groupId, userId)));
    }

    @GetMapping("/me")
    @Operation(summary = "List all groups I belong to")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> mine(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(groupService.listMyGroups(userId)));
    }

    @PostMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Add a user to a group (admin only)")
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(@RequestHeader("X-User-Id") UUID userId,
                                                                @PathVariable UUID groupId,
                                                                @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.ok(groupService.addMember(groupId, userId, memberId)));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Remove a member from a group (admin, or self leave)")
    public ResponseEntity<ApiResponse<Void>> removeMember(@RequestHeader("X-User-Id") UUID userId,
                                                          @PathVariable UUID groupId,
                                                          @PathVariable UUID memberId) {
        groupService.removeMember(groupId, userId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Removed"));
    }

    @PostMapping("/join/{inviteCode}")
    @Operation(summary = "Join a group using an invite code")
    public ResponseEntity<ApiResponse<GroupResponse>> join(@RequestHeader("X-User-Id") UUID userId,
                                                           @PathVariable String inviteCode) {
        return ResponseEntity.ok(ApiResponse.ok(groupService.joinByInviteCode(userId, inviteCode)));
    }
}
