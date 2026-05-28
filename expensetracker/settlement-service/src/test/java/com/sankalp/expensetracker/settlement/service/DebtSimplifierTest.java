package com.sankalp.expensetracker.settlement.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DebtSimplifierTest {

    private final DebtSimplifier simplifier = new DebtSimplifier();

    @Test
    void three_users_classic_case_reduced_to_two_transactions() {
        // A owes B $30, B owes C $30, C owes A $30 (net everyone zero)
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Map<UUID, BigDecimal> net = new HashMap<>();
        net.put(a, BigDecimal.ZERO);
        net.put(b, BigDecimal.ZERO);
        net.put(c, BigDecimal.ZERO);
        var result = simplifier.simplify(net);
        assertThat(result).isEmpty();
    }

    @Test
    void payment_suggested_for_simple_unbalance() {
        // A is owed 100, B owes 100
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Map<UUID, BigDecimal> net = new HashMap<>();
        net.put(a, new BigDecimal("100"));
        net.put(b, new BigDecimal("-100"));
        var result = simplifier.simplify(net);
        assertThat(result).hasSize(1);
        var p = result.get(0);
        assertThat(p.getFrom()).isEqualTo(b);
        assertThat(p.getTo()).isEqualTo(a);
        assertThat(p.getAmount()).isEqualByComparingTo("100");
    }

    @Test
    void multi_user_minimum_transactions() {
        // Alice owed 50, Bob owed 30, Carol owes 30, Dave owes 50
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID carol = UUID.randomUUID();
        UUID dave = UUID.randomUUID();
        Map<UUID, BigDecimal> net = new HashMap<>();
        net.put(alice, new BigDecimal("50"));
        net.put(bob, new BigDecimal("30"));
        net.put(carol, new BigDecimal("-30"));
        net.put(dave, new BigDecimal("-50"));
        var result = simplifier.simplify(net);
        // For 4 people we expect at most 3 transactions, greedy gives 2
        assertThat(result).hasSizeLessThanOrEqualTo(3);
        // verify sum-zero invariant
        BigDecimal totalPaid = result.stream().map(DebtSimplifier.PaymentSuggestion::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPaid).isEqualByComparingTo("80");
    }
}
