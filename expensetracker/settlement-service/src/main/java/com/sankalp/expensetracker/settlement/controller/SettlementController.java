package com.sankalp.expensetracker.settlement.controller;

import com.sankalp.expensetracker.common.dto.ApiResponse;
import com.sankalp.expensetracker.settlement.dto.CreateSettlementRequest;
import com.sankalp.expensetracker.settlement.entity.Balance;
import com.sankalp.expensetracker.settlement.entity.Settlement;
import com.sankalp.expensetracker.settlement.service.BalanceService;
import com.sankalp.expensetracker.settlement.service.DebtSimplifier;
import com.sankalp.expensetracker.settlement.service.SettlementService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Settlements & Balances", description = "Record payments, view balances, get simplified suggestions")
public class SettlementController {

    private final SettlementService settlementService;
    private final BalanceService balanceService;

    @PostMapping("/api/settlements")
    @Operation(summary = "Record a payment between two users (full or partial settlement)")
    public ResponseEntity<ApiResponse<Settlement>> record(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateSettlementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(settlementService.record(userId, req)));
    }

    @GetMapping("/api/settlements/me")
    @Operation(summary = "List my settlement history")
    public ResponseEntity<ApiResponse<Page<Settlement>>> myHistory(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(settlementService.history(userId, pageable)));
    }

    @GetMapping("/api/settlements/group/{groupId}")
    @Operation(summary = "List settlements in a group")
    public ResponseEntity<ApiResponse<Page<Settlement>>> groupHistory(
            @PathVariable UUID groupId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(settlementService.groupHistory(groupId, pageable)));
    }

    @GetMapping("/api/balances/group/{groupId}")
    @Operation(summary = "Get all pairwise balances in a group")
    public ResponseEntity<ApiResponse<List<Balance>>> groupBalances(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.ok(balanceService.getGroupBalances(groupId)));
    }

    @GetMapping("/api/balances/group/{groupId}/suggestions")
    @Operation(summary = "Get minimum-transaction settlement suggestions")
    public ResponseEntity<ApiResponse<List<DebtSimplifier.PaymentSuggestion>>> suggest(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiResponse.ok(settlementService.suggestPayments(groupId)));
    }
}
