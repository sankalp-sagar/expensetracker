package com.sankalp.expensetracker.expense.dto;

import com.sankalp.expensetracker.expense.entity.Receipt;

import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID id,
        UUID expenseId,
        String fileName,
        String contentType,
        Long sizeBytes,
        String storageKey,
        String url,
        String ocrText,
        boolean ocrProcessed,
        Instant createdAt
) {
    public static ReceiptResponse from(Receipt r, String url) {
        return new ReceiptResponse(
                r.getId(),
                r.getExpense().getId(),
                r.getFileName(),
                r.getContentType(),
                r.getSizeBytes(),
                r.getStorageKey(),
                url,
                r.getOcrText(),
                r.isOcrProcessed(),
                r.getCreatedAt());
    }
}
