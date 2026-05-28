package com.sankalp.expensetracker.expense.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.expense.dto.CreateExpenseRequest;
import com.sankalp.expensetracker.expense.dto.ExpenseResponse;
import com.sankalp.expensetracker.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense CRUD, split logic, recurring")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Create a new expense (with computed splits)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateExpenseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(expenseService.createExpense(userId, req)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an expense by id")
    public ResponseEntity<ApiResponse<ExpenseResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.get(id)));
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "List expenses for a group")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> listByGroup(
            @PathVariable UUID groupId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.listByGroup(groupId, pageable)));
    }

    @GetMapping("/me")
    @Operation(summary = "List expenses I'm involved in (payer or split member)")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> listMine(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.listMyExpenses(userId, pageable)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an expense (payer only)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        expenseService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deleted"));
    }
}
