package com.sankalp.expensetracker.settlement.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.settlement.dto.CreateSettlementRequest;
import com.sankalp.expensetracker.settlement.entity.Settlement;
import com.sankalp.expensetracker.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepo;
    private final BalanceService balanceService;
    private final DebtSimplifier debtSimplifier;
    private final KafkaTemplate<String, Object> kafka;

    @Transactional
    public Settlement record(UUID actorId, CreateSettlementRequest req) {
        if (req.amount().signum() <= 0) throw new BusinessException("Amount must be > 0");
        if (req.payerId().equals(req.payeeId())) throw new BusinessException("Payer and payee must differ");
        if (!actorId.equals(req.payerId()) && !actorId.equals(req.payeeId())) {
            throw new BusinessException("Only the payer or payee can record this settlement");
        }
        String currency = req.currency() == null ? "USD" : req.currency();
        BigDecimal outstanding = balanceService.debtOwed(req.groupId(), req.payerId(), req.payeeId(), currency);
        if (outstanding.compareTo(req.amount()) < 0) {
            throw new BusinessException("Settlement exceeds outstanding debt. Outstanding: " + outstanding);
        }

        Settlement s = Settlement.builder()
                .groupId(req.groupId())
                .payerId(req.payerId())
                .payeeId(req.payeeId())
                .amount(req.amount())
                .currency(currency)
                .status(Settlement.Status.COMPLETED)
                .settledAt(Instant.now())
                .method(req.method())
                .note(req.note())
                .build();
        settlementRepo.save(s);

        balanceService.applySettlement(s.getGroupId(), s.getPayerId(), s.getPayeeId(), s.getAmount(), s.getCurrency());

        kafka.send(KafkaTopics.SETTLEMENT_COMPLETED, new Events.SettlementCompletedEvent(
                s.getId(), s.getPayerId(), s.getPayeeId(), s.getGroupId(),
                s.getAmount(), s.getCurrency(), Instant.now()));
        return s;
    }

    public Page<Settlement> history(UUID userId, Pageable pageable) {
        return settlementRepo.findHistoryFor(userId, pageable);
    }

    public Page<Settlement> groupHistory(UUID groupId, Pageable pageable) {
        return settlementRepo.findByGroupIdOrderBySettledAtDesc(groupId, pageable);
    }

    public List<DebtSimplifier.PaymentSuggestion> suggestPayments(UUID groupId) {
        Map<UUID, BigDecimal> net = balanceService.getNetBalancesByUser(groupId);
        return debtSimplifier.simplify(net);
    }
}
