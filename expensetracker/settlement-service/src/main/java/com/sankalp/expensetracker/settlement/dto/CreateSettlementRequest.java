package com.sankalp.expensetracker.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSettlementRequest(
        UUID groupId,
        @NotNull UUID payerId,
        @NotNull UUID payeeId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Size(max = 30) String method,
        @Size(max = 500) String note
) {}
