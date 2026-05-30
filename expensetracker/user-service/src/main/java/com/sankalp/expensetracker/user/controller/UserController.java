package com.sankalp.expensetracker.user.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.common.exception.UnauthorizedException;
import com.sankalp.expensetracker.common.security.AuthenticatedUser;
import com.sankalp.expensetracker.user.dto.UpdateProfileRequest;
import com.sankalp.expensetracker.user.dto.UserProfileResponse;
import com.sankalp.expensetracker.user.dto.UserSummaryResponse;
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
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Full-Name", required = false) String fullName,
            Authentication authentication) {
        CurrentUser currentUser = currentUser(userId, email, fullName, authentication);
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(
                currentUser.userId(), currentUser.email(), currentUser.fullName())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Full-Name", required = false) String fullName,
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest req) {
        CurrentUser currentUser = currentUser(userId, email, fullName, authentication);
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(
                currentUser.userId(), currentUser.email(), currentUser.fullName(), req)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search visible user profiles")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.search(q, pageable)));
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup lightweight user display profiles by user id")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> lookup(
            @RequestParam List<UUID> userIds) {
        return ResponseEntity.ok(ApiResponse.ok(userService.lookupByUserIds(userIds)));
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

    private CurrentUser currentUser(UUID headerUserId, String headerEmail, String headerFullName,
                                    Authentication authentication) {
        UUID userId = headerUserId;
        String email = clean(headerEmail);
        String fullName = clean(headerFullName);

        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            if (userId == null) userId = principal.userId();
            if (!hasText(email)) email = clean(principal.email());
            if (!hasText(fullName)) fullName = clean(principal.fullName());
        }

        if (userId == null) {
            throw new UnauthorizedException("Authenticated user missing");
        }
        return new CurrentUser(userId, email, fullName);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CurrentUser(UUID userId, String email, String fullName) {}
}
