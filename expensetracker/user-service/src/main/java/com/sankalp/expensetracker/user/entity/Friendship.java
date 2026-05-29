package com.sankalp.expensetracker.user.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "addressee_id"}),
        indexes = {
                @Index(name = "idx_friendship_requester", columnList = "requester_id"),
                @Index(name = "idx_friendship_addressee", columnList = "addressee_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Friendship extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "requester_id", nullable = false, columnDefinition = "uuid")
    private UUID requesterId;

    @Column(name = "addressee_id", nullable = false, columnDefinition = "uuid")
    private UUID addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    public enum Status { PENDING, ACCEPTED, REJECTED, BLOCKED }
}
