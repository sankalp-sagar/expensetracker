package com.sankalp.expensetracker.analytics.service;

import com.sankalp.expensetracker.analytics.entity.ExpenseFact;
import com.sankalp.expensetracker.analytics.repository.ExpenseFactRepository;
import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseFactRepository factRepo;

    @KafkaListener(topics = KafkaTopics.EXPENSE_CREATED, groupId = "analytics-service")
    @Transactional
    @CacheEvict(value = {"monthlyTotals", "groupContributions"}, allEntries = true)
    public void onExpenseCreated(Events.ExpenseCreatedEvent e) {
        if (factRepo.existsByExpenseId(e.expenseId())) return;
        factRepo.save(ExpenseFact.builder()
                .expenseId(e.expenseId())
                .payerId(e.payerId())
                .groupId(e.groupId())
                .amount(e.amount())
                .currency(e.currency())
                .description(e.description())
                .factDate(LocalDate.ofInstant(e.occurredAt(), java.time.ZoneOffset.UTC))
                .build());
    }

    public BigDecimal totalSpent(UUID userId, LocalDate from, LocalDate to) {
        return factRepo.totalSpentByUserInRange(userId, from, to);
    }

    @Cacheable(value = "monthlyTotals", key = "#userId")
    public List<Map<String, Object>> monthly(UUID userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : factRepo.monthlyTotalsByUser(userId)) {
            result.add(Map.of("month", row[0], "total", row[1]));
        }
        return result;
    }

    @Cacheable(value = "groupContributions", key = "#groupId")
    public List<Map<String, Object>> groupContributions(UUID groupId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : factRepo.contributionsByGroup(groupId)) {
            result.add(Map.of("userId", row[0], "total", row[1]));
        }
        return result;
    }
}
