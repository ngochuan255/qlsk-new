package com.qlsk.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReportDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceReportDto {
        private Long eventId;
        private String eventTitle;
        private Integer totalSeats;
        private Integer totalRegistered;
        private Integer totalCheckedIn;
        private Integer totalAbsent;
        private Double attendanceRate; // Tỷ lệ tham dự (%)
        private Double absenceRate;    // Tỷ lệ vắng mặt (%)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialReportDto {
        private Long eventId;
        private String eventTitle;
        private BigDecimal estimatedBudget;
        private BigDecimal totalUsedBudget;
        private BigDecimal remainingBudget;
        private Double budgetUsedRate; // % ngân sách đã chi
        private List<ExpenseDtos.ExpenseResponseDto> expenses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackSummaryDto {
        private Long eventId;
        private String eventTitle;
        private Integer totalReviews;
        private Double averageRating;
        private Map<Integer, Long> ratingDistribution; // 1 -> count, 2 -> count, ..., 5 -> count
        private List<FeedbackDtos.FeedbackResponseDto> recentReviews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardSummaryDto {
        private Long totalEvents;
        private Long activeEvents;
        private Long totalAttendees;
        private Long totalRegistrations;
        private Long totalCheckedIn;
        private BigDecimal totalExpenses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminChartReportDto {
        private Map<String, Long> eventsByStatus;
        private List<String> eventTitles;
        private List<Integer> eventRegistrations;
        private List<Integer> eventCheckIns;
        private BigDecimal totalEstimatedBudget;
        private BigDecimal totalUsedExpenses;
        private Map<Integer, Long> ratingDistribution;
        private Map<String, Long> eventsByCategory;
    }
}
