package com.sankalp.expensetracker.expense.entity;

import com.sankalp.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "receipts", indexes = @Index(name = "idx_receipt_expense", columnList = "expense_id"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Receipt extends AuditableEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;     // local path or s3 object key

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Reserved fields for future OCR pipeline */
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "ocr_processed", nullable = false)
    private boolean ocrProcessed = false;
}
