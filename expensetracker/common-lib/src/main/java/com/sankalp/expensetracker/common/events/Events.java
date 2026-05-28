package com.sankalp.expensetracker.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kafka event payloads — same DTOs are published by producers and consumed by listeners. */
public class Events {

    public record UserRegisteredEvent(
            UUID userId, String email, String fullName, Instant occurredAt) {}

    public record UserInvitedEvent(
            UUID inviterId, String inviteeEmail, UUID groupId, String groupName, Instant occurredAt) {}

    public record GroupCreatedEvent(
            UUID groupId, String groupName, UUID ownerId, List<UUID> memberIds, Instant occurredAt) {}

    public record ExpenseCreatedEvent(
            UUID expenseId, UUID groupId, UUID payerId, BigDecimal amount, String currency,
            String description, List<SplitInfo> splits, Instant occurredAt) {}

    public record ExpenseUpdatedEvent(
            UUID expenseId, UUID groupId, BigDecimal amount, Instant occurredAt) {}

    public record SettlementCompletedEvent(
            UUID settlementId, UUID payerId, UUID payeeId, UUID groupId,
            BigDecimal amount, String currency, Instant occurredAt) {}

    public record SplitInfo(UUID userId, BigDecimal share) {}
}
