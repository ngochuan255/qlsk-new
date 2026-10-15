package com.qlsk.dto;

import com.qlsk.entity.EventStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EventDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventCreateRequest {
        @NotBlank(message = "Tên sự kiện không được để trống")
        private String title;

        private String description;

        @NotNull(message = "Danh mục sự kiện không được để trống")
        private Long categoryId;

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        private LocalDateTime startTime;

        @NotNull(message = "Thời gian kết thúc không được để trống")
        private LocalDateTime endTime;

        @NotBlank(message = "Địa điểm không được để trống")
        private String location;

        @NotNull(message = "Số lượng người tham gia tối đa không được để trống")
        @Min(value = 1, message = "Số lượng người tham gia phải lớn hơn 0")
        private Integer maxAttendees;

        private BigDecimal estimatedBudget;

        private String bannerUrl;

        private Boolean hasSeatMap;
        private Integer seatRows;
        private Integer seatCols;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventUpdateRequest {
        private String title;
        private String description;
        private Long categoryId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String location;
        private Integer maxAttendees;
        private BigDecimal estimatedBudget;
        private String bannerUrl;
        private EventStatus status;
        private Boolean hasSeatMap;
        private Integer seatRows;
        private Integer seatCols;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventResponseDto {
        private Long id;
        private String title;
        private String description;
        private Long categoryId;
        private String categoryName;
        private Long organizerId;
        private String organizerName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String location;
        private Integer maxAttendees;
        private Integer registeredAttendees;
        private Boolean canModify;
        private String modificationBlockedReason;
        private Integer remainingSeats;
        private BigDecimal estimatedBudget;
        private EventStatus status;
        private String statusText;
        private String bannerUrl;
        private Boolean hasSeatMap;
        private Integer seatRows;
        private Integer seatCols;
        private Double averageRating;
        private Integer feedbackCount;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatDto {
        private Long id;
        private Long eventId;
        private String seatCode;
        private Integer seatRow;
        private Integer seatCol;
        private String seatType;
        private Boolean isBooked;
    }
}
