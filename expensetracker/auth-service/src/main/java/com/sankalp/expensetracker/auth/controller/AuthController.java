package com.sankalp.expensetracker.auth.controller;

import com.sankalp.expensetracker.auth.dto.LoginRequest;
import com.sankalp.expensetracker.auth.dto.RefreshTokenRequest;
import com.sankalp.expensetracker.auth.dto.RegisterRequest;
import com.sankalp.expensetracker.auth.dto.TokenResponse;
import com.sankalp.expensetracker.auth.service.AuthService;
import com.sankalp.expensetracker.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, JWT refresh and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authService.register(req), "User registered"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain access + refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req), "Logged in"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token, obtain a new access + refresh pair")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(req)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke current access token and all refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest req,
                                                    @RequestHeader("X-User-Id") UUID userId) {
        String auth = req.getHeader("Authorization");
        String token = auth == null ? "" : auth.replaceFirst("(?i)^Bearer ", "");
        authService.logout(token, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out"));
    }

    @GetMapping("/me")
    @Operation(summary = "Echo back JWT subject from gateway headers (sanity check)")
    public ResponseEntity<ApiResponse<Object>> me(@RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("userId", userId, "email", email)));
    }
}
