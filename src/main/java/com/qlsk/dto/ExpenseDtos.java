package com.qlsk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseRequest {
        @NotNull(message = "ID sự kiện không được để trống")
        private Long eventId;

        @NotBlank(message = "Tên khoản chi không được để trống")
        private String title;

        private String description;

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền phải lớn hơn 0")
        private BigDecimal amount;

        @NotNull(message = "Ngày chi không được để trống")
        private LocalDate expenseDate;

        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseResponseDto {
        private Long id;
        private Long eventId;
        private String title;
        private String description;
        private BigDecimal amount;
        private LocalDate expenseDate;
        private String note;
        private String createdByName;
        private LocalDateTime createdAt;
    }
}
