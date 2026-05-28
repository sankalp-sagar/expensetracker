package com.sankalp.expensetracker.notification.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.notification.entity.Notification;
import com.sankalp.expensetracker.notification.service.NotificationEventListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "List and manage user notifications")
public class NotificationController {

    private final NotificationEventListener service;

    @GetMapping
    @Operation(summary = "List my recent notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> list(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(service.recent(userId)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unread(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", service.unreadCount(userId))));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@RequestHeader("X-User-Id") UUID userId,
                                                     @PathVariable UUID id) {
        service.markRead(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
