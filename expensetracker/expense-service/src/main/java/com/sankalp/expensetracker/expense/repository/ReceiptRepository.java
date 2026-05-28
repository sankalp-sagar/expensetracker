package com.sankalp.expensetracker.expense.repository;

import com.sankalp.expensetracker.expense.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByExpenseId(UUID expenseId);
}
