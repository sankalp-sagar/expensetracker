package com.sankalp.expensetracker.common.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, List<String> roles) {}
