package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.common.exception.BusinessException;
import com.sankalp.expensetracker.common.exception.NotFoundException;
import com.sankalp.expensetracker.expense.dto.ReceiptResponse;
import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.entity.Receipt;
import com.sankalp.expensetracker.expense.ocr.OcrProvider;
import com.sankalp.expensetracker.expense.repository.ExpenseRepository;
import com.sankalp.expensetracker.expense.repository.ReceiptRepository;
import com.sankalp.expensetracker.expense.storage.FileStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepo;
    private final ExpenseRepository expenseRepo;
    private final FileStorageProvider storage;
    private final OcrProvider ocrProvider;

    @Transactional
    public ReceiptResponse upload(UUID actorId, UUID expenseId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("Receipt file is empty");
        }
        Expense e = expenseRepo.findByIdAndDeletedFalse(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
        boolean involved = e.getPayerId().equals(actorId)
                || e.getSplits().stream().anyMatch(s -> s.getUserId().equals(actorId));
        if (!involved) {
            throw new BusinessException("Only participants can upload receipts for this expense");
        }
        // Cache bytes so we can both store and OCR them without re-reading the stream
        byte[] bytes = file.getBytes();
        String key = storage.store(file, "receipts/" + expenseId);

        Receipt r = Receipt.builder()
                .expense(e)
                .storageKey(key)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();

        if (ocrProvider.enabled() && isImage(file.getContentType())) {
            try {
                String text = ocrProvider.extractText(new ByteArrayInputStream(bytes), file.getContentType());
                r.setOcrText(text == null ? "" : text);
                r.setOcrProcessed(true);
            } catch (Exception ex) {
                log.warn("OCR pipeline failed for new receipt: {}", ex.getMessage());
            }
        }
        Receipt saved = receiptRepo.save(r);
        return ReceiptResponse.from(saved, storage.resolveUrl(saved.getStorageKey()));
    }

    private boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
