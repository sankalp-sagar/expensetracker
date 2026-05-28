package com.sankalp.expensetracker.expense.dto;

import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.entity.ExpenseSplit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID groupId,
        UUID payerId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String categoryName,
        String splitType,
        boolean recurring,
        String recurrencePeriod,
        LocalDate nextOccurrence,
        String notes,
        List<SplitDto> splits,
        Instant createdAt
) {
    public record SplitDto(UUID userId, BigDecimal amount) {}

    public static ExpenseResponse from(Expense e) {
        List<SplitDto> splits = e.getSplits().stream()
                .map(s -> new SplitDto(s.getUserId(), s.getAmount()))
                .toList();
        return new ExpenseResponse(
                e.getId(), e.getGroupId(), e.getPayerId(), e.getDescription(),
                e.getAmount(), e.getCurrency(), e.getExpenseDate(),
                e.getCategory() == null ? null : e.getCategory().getName(),
                e.getSplitType().name(),
                e.isRecurring(),
                e.getRecurrencePeriod() == null ? null : e.getRecurrencePeriod().name(),
                e.getNextOccurrence(), e.getNotes(),
                splits, e.getCreatedAt());
    }
}
