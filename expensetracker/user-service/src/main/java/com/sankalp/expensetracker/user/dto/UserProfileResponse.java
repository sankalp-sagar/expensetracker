package com.sankalp.expensetracker.user.dto;

import com.sankalp.expensetracker.user.entity.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String avatarUrl,
        String statusMessage,
        String phone,
        String preferredCurrency,
        String preferredLanguage,
        String privacy,
        Instant createdAt
) {
    public static UserProfileResponse from(UserProfile p) {
        return new UserProfileResponse(
                p.getId(), p.getUserId(), p.getEmail(), p.getFullName(),
                p.getAvatarUrl(), p.getStatusMessage(), p.getPhone(),
                p.getPreferredCurrency(), p.getPreferredLanguage(),
                p.getPrivacy().name(), p.getCreatedAt());
    }
}
