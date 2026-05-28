package com.sankalp.expensetracker.auth.dto;

import java.util.List;
import java.util.UUID;

public record TokenResponse(
        UUID userId,
        String email,
        String fullName,
        List<String> roles,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {}
