package com.sankalp.expensetracker.analytics.repository;

import com.sankalp.expensetracker.analytics.entity.ExpenseFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseFactRepository extends JpaRepository<ExpenseFact, UUID> {

    boolean existsByExpenseId(UUID expenseId);

    void deleteByExpenseId(UUID expenseId);

    @Query("select coalesce(sum(f.amount), 0) from ExpenseFact f where f.payerId = :userId and f.factDate between :from and :to")
    BigDecimal totalSpentByUserInRange(@Param("userId") UUID userId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query("""
            select function('to_char', f.factDate, 'YYYY-MM') as month, sum(f.amount) as total
            from ExpenseFact f
            where f.payerId = :userId
            group by function('to_char', f.factDate, 'YYYY-MM')
            order by month desc
            """)
    List<Object[]> monthlyTotalsByUser(@Param("userId") UUID userId);

    @Query("""
            select f.payerId, sum(f.amount)
            from ExpenseFact f
            where f.groupId = :groupId
            group by f.payerId
            """)
    List<Object[]> contributionsByGroup(@Param("groupId") UUID groupId);
}
