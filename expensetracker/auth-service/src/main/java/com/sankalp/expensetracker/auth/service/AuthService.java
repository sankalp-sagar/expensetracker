package com.sankalp.expensetracker.auth.service;

import com.sankalp.expensetracker.auth.dto.LoginRequest;
import com.sankalp.expensetracker.auth.dto.RefreshTokenRequest;
import com.sankalp.expensetracker.auth.dto.RegisterRequest;
import com.sankalp.expensetracker.auth.dto.TokenResponse;
import com.sankalp.expensetracker.auth.entity.RefreshToken;
import com.sankalp.expensetracker.auth.entity.Role;
import com.sankalp.expensetracker.auth.entity.UserCredential;
import com.sankalp.expensetracker.auth.repository.RefreshTokenRepository;
import com.sankalp.expensetracker.auth.repository.RoleRepository;
import com.sankalp.expensetracker.auth.repository.UserCredentialRepository;
import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.common.exception.UnauthorizedException;
import com.sankalp.expensetracker.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserCredentialRepository userRepo;
    private final RoleRepository roleRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redis;

    @Value("${app.jwt.access-ttl-ms}")
    private long accessTtlMs;

    @Value("${app.jwt.refresh-ttl-ms}")
    private long refreshTtlMs;

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepo.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("Email already registered");
        }
        Role userRole = roleRepo.findByName("USER")
                .orElseGet(() -> roleRepo.save(Role.builder().name("USER").build()));

        UserCredential u = UserCredential.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .emailVerified(false)
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        userRepo.save(u);

        kafkaTemplate.send(KafkaTopics.USER_REGISTERED,
                new Events.UserRegisteredEvent(u.getId(), u.getEmail(), u.getFullName(), Instant.now()));

        return issueTokens(u);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        UserCredential u = userRepo.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!u.isEnabled()) throw new UnauthorizedException("Account disabled");
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            userRepo.save(u);
            throw new UnauthorizedException("Invalid credentials");
        }
        u.setFailedLoginAttempts(0);
        userRepo.save(u);
        return issueTokens(u);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest req) {
        String hash = sha256(req.refreshToken());
        RefreshToken stored = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshRepo.save(stored);

        UserCredential u = userRepo.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return issueTokens(u);
    }

    @Transactional
    public void logout(String accessToken, UUID userId) {
        // Blacklist current access token until its natural expiry
        try {
            var exp = jwtUtil.parse(accessToken).getPayload().getExpiration();
            long ttl = exp.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redis.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", Duration.ofMillis(ttl));
            }
        } catch (Exception ignored) { /* token already invalid */ }
        refreshRepo.revokeAllForUser(userId);
    }

    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + token));
    }

    private TokenResponse issueTokens(UserCredential u) {
        List<String> roleNames = u.getRoles().stream().map(Role::getName).toList();
        String access = jwtUtil.generateAccessToken(u.getId(), u.getEmail(), roleNames);
        String refresh = jwtUtil.generateRefreshToken(u.getId(), u.getEmail());
        RefreshToken r = RefreshToken.builder()
                .userId(u.getId())
                .tokenHash(sha256(refresh))
                .expiresAt(Instant.now().plusMillis(refreshTtlMs))
                .revoked(false)
                .build();
        refreshRepo.save(r);
        return new TokenResponse(u.getId(), u.getEmail(), u.getFullName(),
                roleNames, access, refresh, accessTtlMs / 1000);
    }

    private static String sha256(String s) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
