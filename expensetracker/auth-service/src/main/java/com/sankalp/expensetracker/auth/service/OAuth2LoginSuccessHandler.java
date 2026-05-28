package com.sankalp.expensetracker.auth.service;

import com.sankalp.expensetracker.auth.entity.Role;
import com.sankalp.expensetracker.auth.entity.UserCredential;
import com.sankalp.expensetracker.auth.repository.RoleRepository;
import com.sankalp.expensetracker.auth.repository.UserCredentialRepository;
import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs when an OAuth2 (Google) login succeeds.
 *   1. Find or auto-create the local UserCredential.
 *   2. Issue our standard JWT pair.
 *   3. Redirect to the configured frontend URL with tokens in fragment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserCredentialRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafka;

    @Value("${app.oauth2.success-redirect:http://localhost:3000/oauth2/callback}")
    private String frontendRedirect;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication auth) throws IOException {
        if (!(auth.getPrincipal() instanceof OAuth2User oauthUser)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String email = oauthUser.getAttribute("email");
        String name  = oauthUser.getAttribute("name");
        if (email == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google profile missing email");
            return;
        }

        UserCredential user = userRepo.findByEmailIgnoreCase(email).orElseGet(() -> {
            Role userRole = roleRepo.findByName("USER")
                    .orElseGet(() -> roleRepo.save(Role.builder().name("USER").build()));
            UserCredential created = UserCredential.builder()
                    .email(email.toLowerCase())
                    // Random hash — the user can't password-login until they reset.
                    .passwordHash(passwordEncoder.encode("oauth2:" + java.util.UUID.randomUUID()))
                    .fullName(name == null ? email : name)
                    .emailVerified(true)
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(userRole)))
                    .build();
            userRepo.save(created);
            kafka.send(KafkaTopics.USER_REGISTERED,
                    new Events.UserRegisteredEvent(created.getId(), created.getEmail(),
                            created.getFullName(), Instant.now()));
            return created;
        });

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String access  = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Tokens passed in URL fragment so they never hit server logs / referer headers.
        String target = frontendRedirect
                + "#access_token=" + URLEncoder.encode(access, StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(refresh, StandardCharsets.UTF_8)
                + "&user_id=" + user.getId();
        res.sendRedirect(target);
        log.info("OAuth2 success for {}, redirecting to frontend", email);
    }
}
