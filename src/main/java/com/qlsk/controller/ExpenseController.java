package com.qlsk.controller;

import com.qlsk.dto.ExpenseDtos.*;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizer/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> addExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.addExpense(principal.getId(), request));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ExpenseResponseDto>> getEventExpenses(@PathVariable Long eventId) {
        return ResponseEntity.ok(expenseService.getEventExpenses(eventId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa khoản chi thành công"));
    }
}
