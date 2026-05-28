package com.sankalp.expensetracker.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String avatarUrl,
        @Pattern(regexp = "^[A-Z]{3}$") String defaultCurrency,
        List<UUID> initialMemberIds
) {}
