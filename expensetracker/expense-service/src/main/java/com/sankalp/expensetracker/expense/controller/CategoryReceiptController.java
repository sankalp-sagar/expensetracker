package com.sankalp.expensetracker.expense.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.expense.dto.ReceiptResponse;
import com.sankalp.expensetracker.expense.entity.Category;
import com.sankalp.expensetracker.expense.repository.CategoryRepository;
import com.sankalp.expensetracker.expense.service.ReceiptService;
import com.sankalp.expensetracker.expense.storage.FileStorageProvider;
import com.sankalp.expensetracker.expense.storage.LocalFileStorageProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Categories & Receipts", description = "Expense categories and receipt uploads")
public class CategoryReceiptController {

    private final CategoryRepository categoryRepo;
    private final ReceiptService receiptService;
    private final FileStorageProvider storage;

    @GetMapping("/api/categories")
    @Operation(summary = "List categories visible to current user")
    public ResponseEntity<ApiResponse<List<Category>>> list(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(categoryRepo.findVisible(userId)));
    }

    @PostMapping("/api/categories")
    @Operation(summary = "Create a category (owned by current user)")
    public ResponseEntity<ApiResponse<Category>> create(@RequestHeader("X-User-Id") UUID userId,
                                                        @RequestBody Category c) {
        c.setOwnerId(userId);
        return ResponseEntity.ok(ApiResponse.ok(categoryRepo.save(c)));
    }

    @PostMapping(value = "/api/receipts/{expenseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a receipt for an expense")
    public ResponseEntity<ApiResponse<ReceiptResponse>> upload(@RequestHeader("X-User-Id") UUID userId,
                                                               @PathVariable UUID expenseId,
                                                               @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(receiptService.upload(userId, expenseId, file)));
    }

    /** Stream a receipt file. Storage key (everything after /file/) maps to the local path. */
    @GetMapping("/api/receipts/file/**")
    @Operation(summary = "Stream a receipt file by storage key (local provider only)")
    public ResponseEntity<Resource> file(HttpServletRequest request) {
        if (!(storage instanceof LocalFileStorageProvider localStorage)) {
            return ResponseEntity.notFound().build();
        }
        String path = request.getRequestURI();
        int idx = path.indexOf("/file/");
        if (idx < 0) return ResponseEntity.badRequest().build();
        String storageKey = path.substring(idx + "/file/".length());
        Path p;
        try {
            p = localStorage.resolveLocal(storageKey);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        if (!p.toFile().exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new FileSystemResource(p.toFile()));
    }
}
