package com.sankalp.expensetracker.expense.config;

import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.security.CorrelationIdFilter;
import com.sankalp.expensetracker.common.security.JwtAuthenticationFilter;
import com.sankalp.expensetracker.common.security.JwtUtil;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean public NewTopic expenseCreated() { return new NewTopic(KafkaTopics.EXPENSE_CREATED, 3, (short) 1); }
    @Bean public NewTopic expenseUpdated() { return new NewTopic(KafkaTopics.EXPENSE_UPDATED, 3, (short) 1); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/receipts/file/**").permitAll() // file streaming public via gateway-issued URL
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CorrelationIdFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }
}
