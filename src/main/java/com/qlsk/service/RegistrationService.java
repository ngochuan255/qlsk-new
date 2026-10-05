package com.qlsk.service;

import com.qlsk.dto.RegistrationDtos.*;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final CheckInRepository checkInRepository;
    private final QrCodeService qrCodeService;

    @Transactional
    public TicketDto registerEvent(Long userId, RegisterEventRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getRole() != null && "ROLE_ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Tài khoản Quản trị viên (Admin) không tham gia đăng ký sự kiện!");
        }

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        if (event.getStatus() != EventStatus.OPEN_FOR_REGISTRATION) {
            throw new RuntimeException("Sự kiện hiện không mở đăng ký (Trạng thái: " + event.getStatus() + ")");
        }

        // Check if user already registered
        boolean alreadyRegistered = registrationRepository.existsByEventIdAndUserIdAndStatus(
                event.getId(), user.getId(), RegistrationStatus.CONFIRMED);
        if (alreadyRegistered) {
            throw new RuntimeException("Bạn đã đăng ký tham gia sự kiện này rồi!");
        }

        // Check max attendees capacity
        long confirmedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
        if (confirmedCount >= event.getMaxAttendees()) {
            event.setStatus(EventStatus.FULL);
            eventRepository.save(event);
            throw new RuntimeException("Sự kiện đã đủ số lượng người tham gia!");
        }

        // Check and book seat if applicable
        Seat seat = null;
        if (request.getSeatId() != null) {
            seat = seatRepository.findById(request.getSeatId())
                    .orElseThrow(() -> new RuntimeException("Chỗ ngồi không tồn tại"));

            if (!seat.getEvent().getId().equals(event.getId())) {
                throw new RuntimeException("Chỗ ngồi không thuộc sự kiện này");
            }

            if (Boolean.TRUE.equals(seat.getIsBooked())) {
                throw new RuntimeException("Chỗ ngồi " + seat.getSeatCode() + " đã có người chọn");
            }

            seat.setIsBooked(true);
            seatRepository.save(seat);
        }

        String regCode = "REG-" + event.getId() + "-" + System.currentTimeMillis() % 1000000;
        String qrToken = "QLSK-EVT" + event.getId() + "-TKT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Registration registration = Registration.builder()
                .registrationCode(regCode)
                .event(event)
                .user(user)
                .seat(seat)
                .qrCodeToken(qrToken)
                .status(RegistrationStatus.CONFIRMED)
                .build();

        Registration saved = registrationRepository.save(registration);

        // If capacity reached after this registration, mark as FULL
        if (confirmedCount + 1 >= event.getMaxAttendees()) {
            event.setStatus(EventStatus.FULL);
            eventRepository.save(event);
        }

        return toTicketDto(saved);
    }

    @Transactional
    public void cancelRegistration(Long userId, Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lượt đăng ký"));

        if (!reg.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy lượt đăng ký này");
        }

        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw new RuntimeException("Lượt đăng ký này đã được hủy trước đó");
        }

        // Check if already checked in
        if (checkInRepository.existsByRegistrationId(reg.getId())) {
            throw new RuntimeException("Vé này đã được check-in, không thể hủy đăng ký");
        }

        // Release seat if booked
        if (reg.getSeat() != null) {
            Seat seat = reg.getSeat();
            seat.setIsBooked(false);
            seatRepository.save(seat);
        }

        reg.setStatus(RegistrationStatus.CANCELLED);
        reg.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(reg);

        // If event was FULL, reopen registration
        Event event = reg.getEvent();
        if (event.getStatus() == EventStatus.FULL) {
            event.setStatus(EventStatus.OPEN_FOR_REGISTRATION);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void adminCancelRegistration(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lượt đăng ký"));

        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw new RuntimeException("Lượt đăng ký này đã được hủy trước đó");
        }

        // Release seat if booked
        if (reg.getSeat() != null) {
            Seat seat = reg.getSeat();
            seat.setIsBooked(false);
            seatRepository.save(seat);
        }

        reg.setStatus(RegistrationStatus.CANCELLED);
        reg.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(reg);

        // If event was FULL, reopen registration
        Event event = reg.getEvent();
        if (event.getStatus() == EventStatus.FULL) {
            event.setStatus(EventStatus.OPEN_FOR_REGISTRATION);
            eventRepository.save(event);
        }
    }

    @Transactional
    public List<TicketDto> getUserTickets(Long userId) {
        List<Registration> userRegs = registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId);
        List<Registration> activeRegs = new ArrayList<>();

        for (Registration reg : userRegs) {
            Event event = reg.getEvent();
            if (event.getStatus() == EventStatus.COMPLETED) {
                // Tự động xóa vé khi sự kiện đã kết thúc
                if (reg.getSeat() != null) {
                    Seat seat = reg.getSeat();
                    seat.setIsBooked(false);
                    seatRepository.save(seat);
                }
                checkInRepository.findByRegistrationId(reg.getId()).ifPresent(checkInRepository::delete);
                registrationRepository.delete(reg);
            } else {
                activeRegs.add(reg);
            }
        }

        return activeRegs.stream()
                .map(this::toTicketDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getOrganizerEventRegistrations(Long organizerId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        if (event.getOrganizer() == null || !event.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedException("Bạn chỉ có thể xem danh sách đăng ký của sự kiện do mình tổ chức");
        }

        return getEventRegistrations(eventId);
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getEventRegistrations(Long eventId) {
        return registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId).stream()
                .map(this::toTicketDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketDto getTicketByCode(String code) {
        Registration reg = registrationRepository.findByRegistrationCode(code)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vé"));
        return toTicketDto(reg);
    }

    public TicketDto toTicketDto(Registration reg) {
        boolean isCheckedIn = checkInRepository.existsByRegistrationId(reg.getId());
        LocalDateTime checkInTime = null;
        if (isCheckedIn) {
            checkInTime = checkInRepository.findByRegistrationId(reg.getId())
                    .map(CheckIn::getCheckInTime)
                    .orElse(null);
        }

        String qrBase64 = null;
        if (reg.getStatus() == RegistrationStatus.CONFIRMED) {
            qrBase64 = qrCodeService.generateQrCodeBase64(reg.getQrCodeToken());
        }

        return TicketDto.builder()
                .id(reg.getId())
                .registrationCode(reg.getRegistrationCode())
                .eventId(reg.getEvent().getId())
                .eventTitle(reg.getEvent().getTitle())
                .eventStartTime(reg.getEvent().getStartTime())
                .eventEndTime(reg.getEvent().getEndTime())
                .eventStatus(reg.getEvent().getStatus() != null ? reg.getEvent().getStatus().name() : null)
                .eventLocation(reg.getEvent().getLocation())
                .userId(reg.getUser().getId())
                .userName(reg.getUser().getFullName())
                .userEmail(reg.getUser().getEmail())
                .userPhone(reg.getUser().getPhone())
                .seatCode(reg.getSeat() != null ? reg.getSeat().getSeatCode() : "Không chọn chỗ trước")
                .qrCodeToken(reg.getQrCodeToken())
                .qrCodeImageBase64(qrBase64)
                .status(reg.getStatus())
                .isCheckedIn(isCheckedIn)
                .checkInTime(checkInTime)
                .registeredAt(reg.getRegisteredAt())
                .cancelledAt(reg.getCancelledAt())
                .build();
    }
}
