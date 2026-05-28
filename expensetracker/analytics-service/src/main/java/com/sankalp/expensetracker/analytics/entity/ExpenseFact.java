package com.sankalp.expensetracker.analytics.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense_facts", indexes = {
        @Index(name = "idx_fact_payer", columnList = "payer_id"),
        @Index(name = "idx_fact_group", columnList = "group_id"),
        @Index(name = "idx_fact_date", columnList = "fact_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseFact extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "expense_id", nullable = false, columnDefinition = "uuid", unique = true)
    private UUID expenseId;

    @Column(name = "payer_id", nullable = false, columnDefinition = "uuid")
    private UUID payerId;

    @Column(name = "group_id", columnDefinition = "uuid")
    private UUID groupId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "fact_date", nullable = false)
    private LocalDate factDate;
}
