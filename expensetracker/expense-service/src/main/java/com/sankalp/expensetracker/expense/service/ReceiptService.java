package com.sankalp.expensetracker.expense.service;

import com.sankalp.expensetracker.common.exception.NotFoundException;
import com.sankalp.expensetracker.expense.entity.Expense;
import com.sankalp.expensetracker.expense.entity.Receipt;
import com.sankalp.expensetracker.expense.repository.ExpenseRepository;
import com.sankalp.expensetracker.expense.repository.ReceiptRepository;
import com.sankalp.expensetracker.expense.storage.FileStorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepo;
    private final ExpenseRepository expenseRepo;
    private final FileStorageProvider storage;

    @Transactional
    public Receipt upload(UUID expenseId, MultipartFile file) throws IOException {
        Expense e = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
        String key = storage.store(file, "receipts/" + expenseId);
        Receipt r = Receipt.builder()
                .expense(e)
                .storageKey(key)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
        return receiptRepo.save(r);
    }
}
