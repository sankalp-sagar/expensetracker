package com.sankalp.expensetracker.expense.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "categories", indexes = @Index(name = "idx_category_name", columnList = "name", unique = true))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    /** null = system default; otherwise user-owned */
    @Column(name = "owner_id", columnDefinition = "uuid")
    private UUID ownerId;
}
