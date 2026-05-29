package com.sankalp.expensetracker.group.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups", indexes = {
        @Index(name = "idx_group_owner", columnList = "owner_id"),
        @Index(name = "idx_group_invite_code", columnList = "invite_code", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseGroup extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "default_currency", length = 3, nullable = false)
    @Builder.Default
    private String defaultCurrency = "USD";

    @Column(name = "invite_code", length = 12, unique = true)
    private String inviteCode;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<GroupMember> members = new HashSet<>();
}
