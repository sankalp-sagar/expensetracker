package com.sankalp.expensetracker.expense.repository;

import com.sankalp.expensetracker.expense.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @EntityGraph(attributePaths = {"splits", "category"})
    Optional<Expense> findByIdAndDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"splits", "category"})
    @Query("""
            select e from Expense e
            where e.groupId = :groupId
              and e.deleted = false
            order by e.expenseDate desc, e.createdAt desc
            """)
    Page<Expense> findByGroup(@Param("groupId") UUID groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"splits", "category"})
    @Query("""
            select distinct e from Expense e
            left join e.splits s
            where e.deleted = false
              and (e.payerId = :userId or s.userId = :userId)
            order by e.expenseDate desc, e.createdAt desc
            """)
    Page<Expense> findInvolvingUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select e from Expense e
            where e.recurring = true
              and e.nextOccurrence <= :date
              and e.deleted = false
            """)
    List<Expense> findDueRecurring(@Param("date") LocalDate date);
}
