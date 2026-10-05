package com.qlsk.dto;

import com.qlsk.entity.RegistrationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class RegistrationDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterEventRequest {
        @NotNull(message = "ID sự kiện không được để trống")
        private Long eventId;

        // Optional: If event has seating map, attendee chooses a seat
        private Long seatId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketDto {
        private Long id;
        private String registrationCode;
        private Long eventId;
        private String eventTitle;
        private LocalDateTime eventStartTime;
        private LocalDateTime eventEndTime;
        private String eventLocation;
        private Long userId;
        private String userName;
        private String userEmail;
        private String userPhone;
        private String seatCode;
        private String qrCodeToken;
        private String qrCodeImageBase64;
        private RegistrationStatus status;
        private Boolean isCheckedIn;
        private LocalDateTime checkInTime;
        private String eventStatus;
        private LocalDateTime registeredAt;
        private LocalDateTime cancelledAt;
    }
}
