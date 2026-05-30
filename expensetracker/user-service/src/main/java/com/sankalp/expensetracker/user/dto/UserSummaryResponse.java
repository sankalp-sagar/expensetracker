package com.sankalp.expensetracker.user.dto;

import com.sankalp.expensetracker.user.entity.UserProfile;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String avatarUrl
) {
    public static UserSummaryResponse from(UserProfile p) {
        return new UserSummaryResponse(
                p.getUserId(),
                p.getFullName(),
                p.getEmail(),
                p.getAvatarUrl());
    }
}
