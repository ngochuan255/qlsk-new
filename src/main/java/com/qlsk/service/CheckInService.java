package com.qlsk.service;

import com.qlsk.dto.CheckInDtos.*;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckInResponseDto processCheckIn(Long operatorId, CheckInRequest request) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người quét"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        // Find registration by QR code token
        Registration registration = registrationRepository.findByQrCodeToken(request.getQrCodeToken().trim())
                .orElseThrow(() -> new RuntimeException("Mã QR không hợp lệ hoặc không tồn tại trong hệ thống!"));

        // Check if registration belongs to this event
        if (!registration.getEvent().getId().equals(event.getId())) {
            throw new RuntimeException("Mã QR này thuộc về sự kiện khác: '" + registration.getEvent().getTitle() + "'");
        }

        // Check if registration is active
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new RuntimeException("Lượt đăng ký này đã bị hủy trước đó!");
        }

        // Check if already checked in
        if (checkInRepository.existsByRegistrationId(registration.getId())) {
            CheckIn existingCheckIn = checkInRepository.findByRegistrationId(registration.getId()).get();
            throw new RuntimeException("Mã QR này ĐÃ ĐƯỢC CHECK-IN lúc " + existingCheckIn.getCheckInTime() + "!");
        }

        // Save check-in
        CheckIn checkIn = CheckIn.builder()
                .registration(registration)
                .event(event)
                .checkInTime(LocalDateTime.now())
                .checkedInBy(operator)
                .note(request.getNote())
                .build();

        CheckIn savedCheckIn = checkInRepository.save(checkIn);

        String seatCode = registration.getSeat() != null ? registration.getSeat().getSeatCode() : "Tự do";

        return CheckInResponseDto.builder()
                .success(true)
                .message("Check-in thành công!")
                .checkInId(savedCheckIn.getId())
                .registrationCode(registration.getRegistrationCode())
                .attendeeName(registration.getUser().getFullName())
                .attendeeEmail(registration.getUser().getEmail())
                .seatCode(seatCode)
                .checkInTime(savedCheckIn.getCheckInTime())
                .build();
    }
}
