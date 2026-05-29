package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.expense.dto.CreateExpenseRequest;
import com.sankalp.expensetracker.expense.entity.Expense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseSplitterTest {

    private final ExpenseSplitter splitter = new ExpenseSplitter();

    private Expense expense(BigDecimal amount) {
        return Expense.builder().amount(amount).build();
    }

    private CreateExpenseRequest req(BigDecimal total,
                                     CreateExpenseRequest.SplitTypeDto type,
                                     List<CreateExpenseRequest.SplitInputDto> splits) {
        return new CreateExpenseRequest(null, UUID.randomUUID(), "test", total,
                "USD", null, null, type, splits, false, null, null);
    }

    @Test
    void equal_split_balances_pennies() {
        Expense e = expense(new BigDecimal("100.00"));
        var splits = splitter.computeSplits(e, req(new BigDecimal("100.00"),
                CreateExpenseRequest.SplitTypeDto.EQUAL,
                List.of(new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), null),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), null),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), null))));
        assertThat(splits).hasSize(3);
        BigDecimal sum = splits.stream().map(s -> s.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    void exact_split_must_sum_to_total() {
        Expense e = expense(new BigDecimal("100.00"));
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        var splits = splitter.computeSplits(e, req(new BigDecimal("100.00"),
                CreateExpenseRequest.SplitTypeDto.EXACT,
                List.of(new CreateExpenseRequest.SplitInputDto(ids.get(0), new BigDecimal("60")),
                        new CreateExpenseRequest.SplitInputDto(ids.get(1), new BigDecimal("40")))));
        assertThat(splits.get(0).getAmount()).isEqualByComparingTo("60.00");
        assertThat(splits.get(1).getAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void exact_split_rejects_mismatched_sum() {
        Expense e = expense(new BigDecimal("100.00"));
        var req = req(new BigDecimal("100.00"), CreateExpenseRequest.SplitTypeDto.EXACT,
                List.of(new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("60")),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("30"))));
        assertThrows(RuntimeException.class, () -> splitter.computeSplits(e, req));
    }

    @Test
    void duplicate_participants_are_rejected() {
        Expense e = expense(new BigDecimal("100.00"));
        UUID userId = UUID.randomUUID();
        var req = req(new BigDecimal("100.00"), CreateExpenseRequest.SplitTypeDto.EQUAL,
                List.of(new CreateExpenseRequest.SplitInputDto(userId, null),
                        new CreateExpenseRequest.SplitInputDto(userId, null)));
        assertThrows(RuntimeException.class, () -> splitter.computeSplits(e, req));
    }

    @Test
    void percentage_split_rejects_non_positive_values() {
        Expense e = expense(new BigDecimal("100.00"));
        var req = req(new BigDecimal("100.00"), CreateExpenseRequest.SplitTypeDto.PERCENTAGE,
                List.of(new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("0")),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("100"))));
        assertThrows(RuntimeException.class, () -> splitter.computeSplits(e, req));
    }

    @Test
    void percentage_split_sums_correctly_with_rounding() {
        Expense e = expense(new BigDecimal("99.99"));
        var splits = splitter.computeSplits(e, req(new BigDecimal("99.99"),
                CreateExpenseRequest.SplitTypeDto.PERCENTAGE,
                List.of(new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("33.33")),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("33.33")),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("33.34")))));
        BigDecimal sum = splits.stream().map(s -> s.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("99.99");
    }

    @Test
    void share_split_proportional() {
        Expense e = expense(new BigDecimal("100.00"));
        var splits = splitter.computeSplits(e, req(new BigDecimal("100.00"),
                CreateExpenseRequest.SplitTypeDto.SHARE,
                List.of(new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("1")),
                        new CreateExpenseRequest.SplitInputDto(UUID.randomUUID(), new BigDecimal("3")))));
        // 1:3 of 100 => 25 / 75
        assertThat(splits.get(0).getAmount()).isEqualByComparingTo("25.00");
        assertThat(splits.get(1).getAmount()).isEqualByComparingTo("75.00");
    }
}
