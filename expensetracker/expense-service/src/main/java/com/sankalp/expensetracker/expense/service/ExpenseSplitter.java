package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.expense.dto.CreateExpenseRequest;
import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.entity.ExpenseSplit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-user owed amount for all 4 split modes.
 * Penny-safe: any rounding remainder is allocated to the first split so totals match exactly.
 */
@Component
public class ExpenseSplitter {

    public List<ExpenseSplit> computeSplits(Expense expense, CreateExpenseRequest req) {
        var inputs = req.splits();
        if (inputs == null || inputs.isEmpty())
            throw new BusinessException("At least one split participant required");

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
        BigDecimal sum = in.stream().map(s -> nonNull(s.value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.subtract(total).abs().compareTo(new BigDecimal("0.01")) > 0)
            throw new BusinessException("Exact splits must sum to total. Got " + sum + " expected " + total);
        return in.stream().map(s -> ExpenseSplit.builder()
                .expense(e).userId(s.userId()).amount(nonNull(s.value()).setScale(2, RoundingMode.HALF_UP))
                .rawValue(s.value())
                .build()).toList();
    }

    private List<ExpenseSplit> percentage(Expense e, List<CreateExpenseRequest.SplitInputDto> in, BigDecimal total) {
        BigDecimal sumPct = in.stream().map(s -> nonNull(s.value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumPct.subtract(new BigDecimal("100")).abs().compareTo(new BigDecimal("0.01")) > 0)
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

    private BigDecimal nonNull(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
