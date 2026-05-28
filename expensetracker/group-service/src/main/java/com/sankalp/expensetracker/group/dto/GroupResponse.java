package com.sankalp.expensetracker.group.dto;

import com.sankalp.expensetracker.group.entity.ExpenseGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        String avatarUrl,
        UUID ownerId,
        String defaultCurrency,
        String inviteCode,
        List<MemberInfo> members,
        Instant createdAt
) {
    public static GroupResponse from(ExpenseGroup g, List<MemberInfo> members) {
        return new GroupResponse(g.getId(), g.getName(), g.getDescription(), g.getAvatarUrl(),
                g.getOwnerId(), g.getDefaultCurrency(), g.getInviteCode(), members, g.getCreatedAt());
    }

    public record MemberInfo(UUID userId, String role) {}
}
