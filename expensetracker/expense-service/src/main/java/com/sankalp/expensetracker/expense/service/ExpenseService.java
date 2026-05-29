package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.common.events.Events;
import com.sankalp.expensetracker.common.events.KafkaTopics;
import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.common.exception.NotFoundException;
import com.sankalp.expensetracker.expense.dto.CreateExpenseRequest;
import com.sankalp.expensetracker.expense.dto.ExpenseResponse;
import com.sankalp.expensetracker.expense.entity.Category;
import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.repository.CategoryRepository;
import com.sankalp.expensetracker.expense.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final CategoryRepository categoryRepo;
    private final ExpenseSplitter splitter;
    private final KafkaTemplate<String, Object> kafka;

    @Transactional
    public ExpenseResponse createExpense(UUID actorId, CreateExpenseRequest req) {
        if (!actorId.equals(req.payerId())) {
            throw new BusinessException("Payer must match the authenticated user");
        }
        Category category = req.categoryId() == null ? null :
                categoryRepo.findById(req.categoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found"));
        if (category != null && category.getOwnerId() != null && !category.getOwnerId().equals(actorId)) {
            throw new BusinessException("Category not visible to this user");
        }

        Expense e = Expense.builder()
                .groupId(req.groupId())
                .payerId(req.payerId())
                .description(req.description())
                .amount(req.amount())
                .currency(req.currency() == null ? "USD" : req.currency())
                .expenseDate(req.expenseDate() == null ? LocalDate.now() : req.expenseDate())
                .category(category)
                .splitType(Expense.SplitType.valueOf(req.splitType().name()))
                .recurring(req.recurring())
                .recurrencePeriod(req.recurrencePeriod() == null ? null :
                        Expense.RecurrencePeriod.valueOf(req.recurrencePeriod().name()))
                .notes(req.notes())
                .build();
        if (req.recurring()) {
            if (req.recurrencePeriod() == null)
                throw new BusinessException("Recurrence period required for recurring expenses");
            e.setNextOccurrence(nextDate(e.getExpenseDate(), e.getRecurrencePeriod()));
        }
        e.getSplits().addAll(splitter.computeSplits(e, req));
        expenseRepo.save(e);

        publishExpenseCreated(e);

        return ExpenseResponse.from(e);
    }

    public ExpenseResponse get(UUID id) {
        return ExpenseResponse.from(
                expenseRepo.findByIdAndDeletedFalse(id).orElseThrow(() -> new NotFoundException("Expense not found")));
    }

    public Page<ExpenseResponse> listByGroup(UUID groupId, Pageable pageable) {
        return expenseRepo.findByGroup(groupId, pageable).map(ExpenseResponse::from);
    }

    public Page<ExpenseResponse> listMyExpenses(UUID userId, Pageable pageable) {
        return expenseRepo.findInvolvingUser(userId, pageable).map(ExpenseResponse::from);
    }

    @Transactional
    public void delete(UUID expenseId, UUID actorId) {
        Expense e = expenseRepo.findByIdAndDeletedFalse(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
        if (!e.getPayerId().equals(actorId))
            throw new BusinessException("Only the payer can delete this expense");
        e.setDeleted(true);
        e.setDeletedAt(Instant.now());
        expenseRepo.save(e);

        kafka.send(KafkaTopics.EXPENSE_DELETED, new Events.ExpenseDeletedEvent(
                e.getId(), e.getGroupId(), e.getPayerId(), e.getCurrency(), splitInfos(e), Instant.now()));
    }

    /** Processes due recurring expenses, materializing the next occurrence. */
    @Transactional
    public int processRecurring(LocalDate today) {
        List<Expense> due = expenseRepo.findDueRecurring(today);
        int created = 0;
        for (Expense template : due) {
            // create a child non-recurring expense for this period
            Expense child = Expense.builder()
                    .groupId(template.getGroupId())
                    .payerId(template.getPayerId())
                    .description(template.getDescription() + " (recurring)")
                    .amount(template.getAmount())
                    .currency(template.getCurrency())
                    .expenseDate(template.getNextOccurrence())
                    .category(template.getCategory())
                    .splitType(template.getSplitType())
                    .notes(template.getNotes())
                    .recurring(false)
                    .build();
            for (var s : template.getSplits()) {
                child.getSplits().add(com.sankalp.expensetracker.expense.entity.ExpenseSplit.builder()
                        .expense(child).userId(s.getUserId())
                        .amount(s.getAmount()).rawValue(s.getRawValue()).build());
            }
            expenseRepo.save(child);
            publishExpenseCreated(child);
            template.setNextOccurrence(nextDate(template.getNextOccurrence(), template.getRecurrencePeriod()));
            expenseRepo.save(template);
            created++;
        }
        return created;
    }

    private LocalDate nextDate(LocalDate from, Expense.RecurrencePeriod period) {
        return switch (period) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
            case YEARLY -> from.plusYears(1);
        };
    }

    private void publishExpenseCreated(Expense e) {
        kafka.send(KafkaTopics.EXPENSE_CREATED,
                new Events.ExpenseCreatedEvent(e.getId(), e.getGroupId(), e.getPayerId(),
                        e.getAmount(), e.getCurrency(), e.getDescription(), e.getExpenseDate(),
                        splitInfos(e), Instant.now()));
    }

    private List<Events.SplitInfo> splitInfos(Expense e) {
        return e.getSplits().stream()
                .map(s -> new Events.SplitInfo(s.getUserId(), s.getAmount()))
                .toList();
    }
}
