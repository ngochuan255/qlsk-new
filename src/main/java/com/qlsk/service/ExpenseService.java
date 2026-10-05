package com.qlsk.service;

import com.qlsk.dto.ExpenseDtos.*;
import com.qlsk.entity.Event;
import com.qlsk.entity.Expense;
import com.qlsk.entity.User;
import com.qlsk.repository.EventRepository;
import com.qlsk.repository.ExpenseRepository;
import com.qlsk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpenseResponseDto addExpense(Long userId, ExpenseRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Expense expense = Expense.builder()
                .event(event)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .note(request.getNote())
                .createdBy(user)
                .build();

        return toDto(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponseDto> getEventExpenses(Long eventId) {
        return expenseRepository.findByEventIdOrderByExpenseDateDesc(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpensesByEvent(Long eventId) {
        return expenseRepository.sumAmountByEventId(eventId);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        expenseRepository.deleteById(expenseId);
    }

    public ExpenseResponseDto toDto(Expense expense) {
        return ExpenseResponseDto.builder()
                .id(expense.getId())
                .eventId(expense.getEvent().getId())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .note(expense.getNote())
                .createdByName(expense.getCreatedBy().getFullName())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
