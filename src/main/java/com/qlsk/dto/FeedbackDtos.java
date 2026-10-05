package com.qlsk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class FeedbackDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackRequest {
        @NotNull(message = "ID sự kiện không được để trống")
        private Long eventId;

        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
        @Max(value = 5, message = "Đánh giá tối đa 5 sao")
        private Integer rating;

        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackResponseDto {
        private Long id;
        private Long eventId;
        private Long userId;
        private String userName;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }
}
