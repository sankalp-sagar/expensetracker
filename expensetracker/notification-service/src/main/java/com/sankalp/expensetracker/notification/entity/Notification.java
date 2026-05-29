package com.sankalp.expensetracker.notification.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_read", columnList = "is_read")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "related_id", columnDefinition = "uuid")
    private UUID relatedId;        // group/expense/settlement id

    public enum NotificationType {
        USER_INVITED, GROUP_CREATED, EXPENSE_CREATED, SETTLEMENT_COMPLETED, GENERIC
    }
}
