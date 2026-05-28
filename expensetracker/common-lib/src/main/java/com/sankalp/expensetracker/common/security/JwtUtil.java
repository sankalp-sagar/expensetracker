package com.sankalp.expensetracker.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared JWT helper. Symmetric HS256. Same secret across all services.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtUtil(
            @Value("${app.jwt.secret:change-me-in-production-must-be-very-long-secret-key-256-bits}") String secret,
            @Value("${app.jwt.access-ttl-ms:3600000}") long accessTtlMs,
            @Value("${app.jwt.refresh-ttl-ms:604800000}") long refreshTtlMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        return build(userId, email, roles, accessTtlMs, "access");
    }

    public String generateRefreshToken(UUID userId, String email) {
        return build(userId, email, List.of(), refreshTtlMs, "refresh");
    }

    private String build(UUID userId, String email, List<String> roles, long ttl, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("email", email, "roles", roles, "type", type))
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getPayload().getSubject());
    }

    public String extractEmail(String token) {
        return (String) parse(token).getPayload().get("email");
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parse(token).getPayload().get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
