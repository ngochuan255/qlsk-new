package com.qlsk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class CheckInDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInRequest {
        @NotNull(message = "ID sự kiện không được để trống")
        private Long eventId;

        @NotBlank(message = "Mã QR / Token không được để trống")
        private String qrCodeToken;

        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckInResponseDto {
        private Boolean success;
        private String message;
        private Long checkInId;
        private String registrationCode;
        private String attendeeName;
        private String attendeeEmail;
        private String seatCode;
        private LocalDateTime checkInTime;
    }
}
