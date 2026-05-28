package com.sankalp.expensetracker.analytics.controller;

import com.sankalp.expensetracker.analytics.service.AnalyticsService;
import com.sankalp.expensetracker.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Spending insights, monthly trends, group contributions")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/spent")
    @Operation(summary = "Total amount I paid in a date range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> totalSpent(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        BigDecimal total = analyticsService.totalSpent(userId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("from", from, "to", to, "total", total)));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Monthly spending trend for current user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> monthly(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.monthly(userId)));
    }

    @GetMapping("/group/{groupId}/contributions")
    @Operation(summary = "Per-user contributions inside a group")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> contributions(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.groupContributions(groupId)));
    }
}
