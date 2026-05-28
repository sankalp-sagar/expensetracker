package com.sankalp.expensetracker.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Pre-routing JWT validation. Public paths skipped; valid tokens add X-User-Id / X-User-Email / X-User-Roles headers
 * to the forwarded request so downstream services can trust without redoing parsing.
 */
@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey key;
    private final List<String> publicPaths;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthGatewayFilter(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.security.public-paths}") String publicPathsStr) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.publicPaths = List.of(publicPathsStr.split(","));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();

        if (publicPaths.stream().anyMatch(p -> matcher.match(p, path))) {
            return chain.filter(exchange);
        }

        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }
        String token = auth.substring(7);
        try {
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims c = jws.getPayload();
            UUID uid = UUID.fromString(c.getSubject());
            String email = (String) c.get("email");
            Object rolesObj = c.get("roles");
            String roles = rolesObj == null ? "" : String.join(",", (List<String>) rolesObj);
            ServerHttpRequest mutated = req.mutate()
                    .header("X-User-Id", uid.toString())
                    .header("X-User-Email", email == null ? "" : email)
                    .header("X-User-Roles", roles)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", msg);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() { return -100; }
}
