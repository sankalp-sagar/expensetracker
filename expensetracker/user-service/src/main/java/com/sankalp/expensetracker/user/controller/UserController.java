package com.sankalp.expensetracker.user.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.user.dto.UpdateProfileRequest;
import com.sankalp.expensetracker.user.dto.UserProfileResponse;
import com.sankalp.expensetracker.user.entity.Friendship;
import com.sankalp.expensetracker.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile, friend graph, search")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(userId)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(userId, req)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search visible user profiles")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.search(q, pageable)));
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get a profile by id")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getById(@PathVariable UUID profileId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(profileId)));
    }

    @PostMapping("/friends/{addresseeId}")
    @Operation(summary = "Send a friend request")
    public ResponseEntity<ApiResponse<Friendship>> addFriend(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID addresseeId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.sendFriendRequest(userId, addresseeId)));
    }

    @PutMapping("/friends/{friendshipId}/respond")
    @Operation(summary = "Accept or reject a friend request")
    public ResponseEntity<ApiResponse<Friendship>> respondFriend(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID friendshipId,
            @RequestParam boolean accept) {
        return ResponseEntity.ok(ApiResponse.ok(userService.respondToFriendRequest(friendshipId, userId, accept)));
    }

    @GetMapping("/friends")
    @Operation(summary = "List my friends")
    public ResponseEntity<ApiResponse<List<Friendship>>> listFriends(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listFriends(userId)));
    }

    @GetMapping("/friends/pending")
    @Operation(summary = "List incoming friend requests")
    public ResponseEntity<ApiResponse<List<Friendship>>> pending(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listPendingRequests(userId)));
    }
}
