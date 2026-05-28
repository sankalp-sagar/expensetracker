package com.sankalp.expensetracker.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 500) String avatarUrl,
        @Size(max = 280) String statusMessage,
        @Size(max = 30) String phone,
        @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 4217 currency, e.g. USD") String preferredCurrency,
        @Size(min = 2, max = 5) String preferredLanguage,
        @Pattern(regexp = "PUBLIC|FRIENDS_ONLY|PRIVATE") String privacy
) {}
