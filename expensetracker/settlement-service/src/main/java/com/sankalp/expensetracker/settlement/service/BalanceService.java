package com.sankalp.expensetracker.settlement.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.settlement.entity.Balance;
import com.sankalp.expensetracker.settlement.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Listens to expense and settlement events, updates pairwise balances.
 * Pair canonicalization: store with smaller-UUID first as userA, so we have at most one row per (groupId, pair, currency).
 * `amount` is signed: positive means userA owes userB; negative means userB owes userA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepo;
    private final SimpMessagingTemplate ws;

    @KafkaListener(topics = KafkaTopics.EXPENSE_CREATED, groupId = "settlement-service")
    @Transactional
    public void onExpenseCreated(Events.ExpenseCreatedEvent e) {
        log.info("Updating balances for expense {} in group {}", e.expenseId(), e.groupId());
        UUID payer = e.payerId();
        for (Events.SplitInfo split : e.splits()) {
            if (split.userId().equals(payer)) continue;       // payer owes themselves: skip
            applyDebt(e.groupId(), split.userId(), payer, split.share(), e.currency());
        }
        publishGroupBalances(e.groupId());
    }

    @Transactional
    public void applySettlement(UUID groupId, UUID payer, UUID payee, BigDecimal amount, String currency) {
        // payer pays payee => debt of payer to payee decreases
        applyDebt(groupId, payer, payee, amount.negate(), currency);
        publishGroupBalances(groupId);
    }

    /** debtor owes creditor `amount` more (positive). */
    private void applyDebt(UUID groupId, UUID debtor, UUID creditor, BigDecimal amount, String currency) {
        UUID a, b;
        BigDecimal signed;
        if (debtor.toString().compareTo(creditor.toString()) < 0) {
            // userA = debtor; positive means debtor owes creditor
            a = debtor; b = creditor; signed = amount;
        } else {
            a = creditor; b = debtor; signed = amount.negate();
        }
        var existing = balanceRepo.findByGroupIdAndUserAAndUserBAndCurrency(groupId, a, b, currency);
        Balance bal = existing.orElseGet(() -> Balance.builder()
                .groupId(groupId).userA(a).userB(b).currency(currency)
                .amount(BigDecimal.ZERO).build());
        bal.setAmount(bal.getAmount().add(signed).setScale(2, RoundingMode.HALF_UP));
        balanceRepo.save(bal);
    }

    public List<Balance> getGroupBalances(UUID groupId) {
        return balanceRepo.findByGroupId(groupId);
    }

    public Map<UUID, BigDecimal> getNetBalancesByUser(UUID groupId) {
        Map<UUID, BigDecimal> net = new HashMap<>();
        for (Balance b : balanceRepo.findByGroupId(groupId)) {
            // userA owes userB `amount`: userA -= amount; userB += amount
            net.merge(b.getUserA(), b.getAmount().negate(), BigDecimal::add);
            net.merge(b.getUserB(), b.getAmount(), BigDecimal::add);
        }
        return net;
    }

    private void publishGroupBalances(UUID groupId) {
        if (groupId == null) return;
        ws.convertAndSend("/topic/balances/" + groupId, getGroupBalances(groupId));
    }
}
