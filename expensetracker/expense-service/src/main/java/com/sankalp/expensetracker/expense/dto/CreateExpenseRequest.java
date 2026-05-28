package com.sankalp.expensetracker.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        UUID groupId,
        @NotNull UUID payerId,
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        LocalDate expenseDate,
        UUID categoryId,
        @NotNull SplitTypeDto splitType,
        @Valid @NotEmpty List<SplitInputDto> splits,
        boolean recurring,
        RecurrencePeriodDto recurrencePeriod,
        @Size(max = 1000) String notes
) {
    public enum SplitTypeDto { EQUAL, EXACT, PERCENTAGE, SHARE }
    public enum RecurrencePeriodDto { DAILY, WEEKLY, MONTHLY, YEARLY }

    public record SplitInputDto(
            @NotNull UUID userId,
            BigDecimal value      // amount for EXACT, percent for PERCENTAGE, shares for SHARE, ignored for EQUAL
    ) {}
}
