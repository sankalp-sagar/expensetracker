package com.sankalp.expensetracker.settlement.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settle_payer", columnList = "payer_id"),
        @Index(name = "idx_settle_payee", columnList = "payee_id"),
        @Index(name = "idx_settle_group", columnList = "group_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Settlement extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "payer_id", nullable = false, columnDefinition = "uuid")
    private UUID payerId;

    @Column(name = "payee_id", nullable = false, columnDefinition = "uuid")
    private UUID payeeId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.COMPLETED;

    @Column(name = "settled_at")
    @Builder.Default
    private Instant settledAt = Instant.now();

    @Column(name = "method", length = 30)
    private String method;       // e.g. CASH, BANK_TRANSFER, UPI

    @Column(length = 500)
    private String note;

    public enum Status { PENDING, COMPLETED, REVERTED }
}
