package com.sankalp.expensetracker.settlement.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Reduces an arbitrary directed-debt graph to the minimum number of payments
 * such that every user's net balance is preserved.
 *
 * Classic greedy algorithm:
 *   1. Compute each user's net balance (sum of credits - sum of debits).
 *   2. Sort: largest creditor, largest debtor.
 *   3. Greedily match max debtor to max creditor with the min of their abs values.
 *
 * This yields up to N-1 transactions for N participants (optimal in practice for typical group sizes).
 */
@Component
public class DebtSimplifier {

    @Getter
    @AllArgsConstructor
    public static class PaymentSuggestion {
        UUID from;       // who pays
        UUID to;         // who receives
        BigDecimal amount;
    }

    /**
     * @param netBalances signed net balance per user: positive => is owed money; negative => owes money
     * @return ordered list of suggested payments
     */
    public List<PaymentSuggestion> simplify(Map<UUID, BigDecimal> netBalances) {
        // separate into credit/debit queues sorted by magnitude
        PriorityQueue<UserAmount> creditors = new PriorityQueue<>(
                (a, b) -> b.amount.compareTo(a.amount));    // largest positive first
        PriorityQueue<UserAmount> debtors = new PriorityQueue<>(
                Comparator.comparing((UserAmount u) -> u.amount));   // most negative first

        for (var e : netBalances.entrySet()) {
            BigDecimal v = e.getValue().setScale(2, RoundingMode.HALF_UP);
            int s = v.signum();
            if (s > 0) creditors.add(new UserAmount(e.getKey(), v));
            else if (s < 0) debtors.add(new UserAmount(e.getKey(), v));
        }

        List<PaymentSuggestion> result = new ArrayList<>();
        BigDecimal CENT = new BigDecimal("0.01");

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            UserAmount cr = creditors.poll();
            UserAmount dr = debtors.poll();
            BigDecimal pay = cr.amount.min(dr.amount.abs());
            if (pay.compareTo(CENT) < 0) break;
            result.add(new PaymentSuggestion(dr.userId, cr.userId, pay));

            BigDecimal crLeft = cr.amount.subtract(pay);
            BigDecimal drLeft = dr.amount.add(pay);
            if (crLeft.compareTo(CENT) >= 0) creditors.add(new UserAmount(cr.userId, crLeft));
            if (drLeft.compareTo(CENT.negate()) <= 0) debtors.add(new UserAmount(dr.userId, drLeft));
        }
        return result;
    }

    private record UserAmount(UUID userId, BigDecimal amount) {}
}
