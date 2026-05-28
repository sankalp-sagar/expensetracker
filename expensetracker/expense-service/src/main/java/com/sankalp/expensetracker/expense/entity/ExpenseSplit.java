package com.sankalp.expensetracker.expense.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "expense_splits", indexes = {
        @Index(name = "idx_split_user", columnList = "user_id"),
        @Index(name = "idx_split_expense", columnList = "expense_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseSplit extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    /** computed amount this user owes for this expense */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** raw input value: exact / percentage / shares */
    @Column(name = "raw_value", precision = 18, scale = 6)
    private BigDecimal rawValue;
}
