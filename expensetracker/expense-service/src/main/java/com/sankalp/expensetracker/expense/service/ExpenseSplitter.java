package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.expense.dto.CreateExpenseRequest;
import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.entity.ExpenseSplit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Computes per-user owed amount for all 4 split modes.
 * Penny-safe: any rounding remainder is allocated to the last split so totals match exactly.
 */
@Component
public class ExpenseSplitter {

    public List<ExpenseSplit> computeSplits(Expense expense, CreateExpenseRequest req) {
        var inputs = req.splits();
        if (inputs == null || inputs.isEmpty())
            throw new BusinessException("At least one split participant required");
        validateUniqueParticipants(inputs);

        return switch (req.splitType()) {
            case EQUAL -> equal(expense, inputs, expense.getAmount());
            case EXACT -> exact(expense, inputs, expense.getAmount());
            case PERCENTAGE -> percentage(expense, inputs, expense.getAmount());
            case SHARE -> share(expense, inputs, expense.getAmount());
        };
    }

    private List<ExpenseSplit> equal(Expense e, List<CreateExpenseRequest.SplitInputDto> in, BigDecimal total) {
        int n = in.size();
        BigDecimal each = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            BigDecimal amt = (i == n - 1) ? total.subtract(running) : each;
            running = running.add(each);
            splits.add(ExpenseSplit.builder().expense(e).userId(in.get(i).userId()).amount(amt).build());
        }
        return splits;
    }

    private List<ExpenseSplit> exact(Expense e, List<CreateExpenseRequest.SplitInputDto> in, BigDecimal total) {
        List<BigDecimal> amounts = in.stream()
                .map(s -> nonNull(s.value()).setScale(2, RoundingMode.HALF_UP))
                .toList();
        if (amounts.stream().anyMatch(v -> v.signum() < 0)) {
            throw new BusinessException("Exact split amounts cannot be negative");
        }
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0)
            throw new BusinessException("Exact splits must sum to total. Got " + sum + " expected " + total);
        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            splits.add(ExpenseSplit.builder()
                    .expense(e).userId(in.get(i).userId()).amount(amounts.get(i))
                    .rawValue(in.get(i).value())
                    .build());
        }
        return splits;
    }

    private List<ExpenseSplit> percentage(Expense e, List<CreateExpenseRequest.SplitInputDto> in, BigDecimal total) {
        BigDecimal sumPct = in.stream().map(s -> nonNull(s.value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (in.stream().map(s -> nonNull(s.value())).anyMatch(v -> v.signum() <= 0)) {
            throw new BusinessException("Percent values must be > 0");
        }
        if (sumPct.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("100.00")) != 0)
            throw new BusinessException("Percent splits must sum to 100. Got " + sumPct);
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < in.size(); i++) {
            BigDecimal pct = nonNull(in.get(i).value());
            BigDecimal amt = (i == in.size() - 1)
                    ? total.subtract(running)
                    : total.multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            running = running.add(amt);
            splits.add(ExpenseSplit.builder().expense(e).userId(in.get(i).userId())
                    .amount(amt).rawValue(pct).build());
        }
        return splits;
    }

    private List<ExpenseSplit> share(Expense e, List<CreateExpenseRequest.SplitInputDto> in, BigDecimal total) {
        BigDecimal sumShares = in.stream().map(s -> nonNull(s.value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (in.stream().map(s -> nonNull(s.value())).anyMatch(v -> v.signum() <= 0)) {
            throw new BusinessException("Shares must be > 0");
        }
        if (sumShares.signum() <= 0) throw new BusinessException("Shares must be > 0");
        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < in.size(); i++) {
            BigDecimal sh = nonNull(in.get(i).value());
            BigDecimal amt = (i == in.size() - 1)
                    ? total.subtract(running)
                    : total.multiply(sh).divide(sumShares, 2, RoundingMode.HALF_UP);
            running = running.add(amt);
            splits.add(ExpenseSplit.builder().expense(e).userId(in.get(i).userId())
                    .amount(amt).rawValue(sh).build());
        }
        return splits;
    }

    private void validateUniqueParticipants(List<CreateExpenseRequest.SplitInputDto> in) {
        Set<java.util.UUID> seen = new HashSet<>();
        for (CreateExpenseRequest.SplitInputDto s : in) {
            if (!seen.add(s.userId())) {
                throw new BusinessException("Split participants must be unique");
            }
        }
    }

    private BigDecimal nonNull(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
