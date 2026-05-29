package com.sankalp.expensetracker.expense.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_group", columnList = "group_id"),
        @Index(name = "idx_expense_payer", columnList = "payer_id"),
        @Index(name = "idx_expense_date", columnList = "expense_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", columnDefinition = "uuid")
    private UUID groupId;       // nullable: personal expense / non-group debt

    @Column(name = "payer_id", nullable = false, columnDefinition = "uuid")
    private UUID payerId;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "expense_date", nullable = false)
    @Builder.Default
    private LocalDate expenseDate = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean recurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_period", length = 20)
    private RecurrencePeriod recurrencePeriod;

    @Column(name = "next_occurrence")
    private LocalDate nextOccurrence;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Receipt> receipts = new ArrayList<>();

    public enum SplitType { EQUAL, EXACT, PERCENTAGE, SHARE }
    public enum RecurrencePeriod { DAILY, WEEKLY, MONTHLY, YEARLY }
}
