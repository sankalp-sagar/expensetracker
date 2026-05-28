package com.sankalp.expensetracker.expense.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class RecurringExpenseScheduler {

    private final ExpenseService expenseService;

    public RecurringExpenseScheduler(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** Run every day at 02:00 server time. */
    @Scheduled(cron = "0 0 2 * * *")
    public void materializeRecurring() {
        int n = expenseService.processRecurring(LocalDate.now());
        if (n > 0) log.info("Recurring expense scheduler materialized {} new expenses", n);
    }
}
