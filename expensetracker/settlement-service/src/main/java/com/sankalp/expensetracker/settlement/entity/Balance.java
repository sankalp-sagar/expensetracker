package com.sankalp.expensetracker.settlement.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_a", "user_b", "currency"}),
        indexes = {
                @Index(name = "idx_balance_group", columnList = "group_id"),
                @Index(name = "idx_balance_a", columnList = "user_a"),
                @Index(name = "idx_balance_b", columnList = "user_b")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Balance extends AuditableEntity {

    /**
     * Stored as a canonical user pair. Positive amount means user_a owes user_b;
     * negative amount means user_b owes user_a.
     * Canonicalize order so we have one row per pair: store with userA.uuid < userB.uuid lexically.
     */

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", columnDefinition = "uuid")
    private UUID groupId;          // null = global / non-group

    @Column(name = "user_a", nullable = false, columnDefinition = "uuid")
    private UUID userA;

    @Column(name = "user_b", nullable = false, columnDefinition = "uuid")
    private UUID userB;

    /** signed: positive => userA owes userB; negative => userB owes userA. */
    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
}
