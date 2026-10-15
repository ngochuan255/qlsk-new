package com.qlsk.service;

import com.qlsk.dto.EventDtos.*;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final RegistrationRepository registrationRepository;
    private final CheckInRepository checkInRepository;
    private final FeedbackRepository feedbackRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public EventResponseDto createEvent(Long organizerId, EventCreateRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ban tổ chức"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .organizer(organizer)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .maxAttendees(request.getMaxAttendees())
                .estimatedBudget(request.getEstimatedBudget())
                .bannerUrl(request.getBannerUrl())
                .hasSeatMap(Boolean.TRUE.equals(request.getHasSeatMap()))
                .seatRows(request.getSeatRows() != null ? request.getSeatRows() : 0)
                .seatCols(request.getSeatCols() != null ? request.getSeatCols() : 0)
                .status(EventStatus.DRAFT)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Generate Seat matrix if seat map is enabled
        if (savedEvent.getHasSeatMap() && savedEvent.getSeatRows() > 0 && savedEvent.getSeatCols() > 0) {
            List<Seat> seats = new ArrayList<>();
            for (int r = 1; r <= savedEvent.getSeatRows(); r++) {
                char rowLetter = (char) ('A' + (r - 1));
                for (int c = 1; c <= savedEvent.getSeatCols(); c++) {
                    String seatCode = rowLetter + String.valueOf(c);
                    String type = (r <= 2) ? "VIP" : "STANDARD";
                    Seat seat = Seat.builder()
                            .event(savedEvent)
                            .seatCode(seatCode)
                            .seatRow(r)
                            .seatCol(c)
                            .seatType(type)
                            .isBooked(false)
                            .build();
                    seats.add(seat);
                }
            }
            seatRepository.saveAll(seats);
            // Sync maxAttendees to seat count
            savedEvent.setMaxAttendees(seats.size());
            savedEvent = eventRepository.save(savedEvent);
        }

        return toDto(savedEvent);
    }

    @Transactional
    public EventResponseDto approveEvent(Long eventId, Long adminId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        assertCanModify(event);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quản trị viên"));

        event.setStatus(EventStatus.OPEN_FOR_REGISTRATION);
        event.setApprovedBy(admin);
        event.setApprovedAt(LocalDateTime.now());

        return toDto(eventRepository.save(event));
    }

    @Transactional
    public EventResponseDto updateEventStatus(Long eventId, EventStatus newStatus, Long actorId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        assertCanManage(event, actorId);
        assertCanModify(event);

        event.setStatus(newStatus);
        return toDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> searchEvents(String keyword, Long categoryId, EventStatus status, LocalDateTime start, LocalDateTime end) {
        return eventRepository.searchEvents(keyword, categoryId, status, start, end).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getEventsByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponseDto> getAllEventsForAdmin() {
        return eventRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponseDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + eventId));
        return toDto(event);
    }

    @Transactional(readOnly = true)
    public List<SeatDto> getEventSeats(Long eventId) {
        return seatRepository.findByEventIdOrderBySeatRowAscSeatColAsc(eventId).stream()
                .map(seat -> SeatDto.builder()
                        .id(seat.getId())
                        .eventId(seat.getEvent().getId())
                        .seatCode(seat.getSeatCode())
                        .seatRow(seat.getSeatRow())
                        .seatCol(seat.getSeatCol())
                        .seatType(seat.getSeatType())
                        .isBooked(seat.getIsBooked())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Automatic lifecycle state transitions based on time
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void updateEventLifecycle() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> openEvents = eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.OPEN_FOR_REGISTRATION);
        for (Event event : openEvents) {
            if (now.isAfter(event.getStartTime()) && now.isBefore(event.getEndTime())) {
                event.setStatus(EventStatus.ONGOING);
                eventRepository.save(event);
            }
        }

        List<Event> ongoingEvents = eventRepository.findByStatusOrderByStartTimeAsc(EventStatus.ONGOING);
        for (Event event : ongoingEvents) {
            if (now.isAfter(event.getEndTime())) {
                event.setStatus(EventStatus.COMPLETED);
                eventRepository.save(event);
            }
        }
    }

    @Transactional
    public EventResponseDto updateEvent(Long eventId, EventUpdateRequest request, Long actorId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + eventId));

        assertCanManage(event, actorId);
        assertCanModify(event);
        if (request.getMaxAttendees() != null) {
            throw new RuntimeException("Không thể thay đổi số lượng người tham gia tối đa khi sửa sự kiện");
        }
        LocalDateTime start = request.getStartTime() != null ? request.getStartTime() : event.getStartTime();
        LocalDateTime end = request.getEndTime() != null ? request.getEndTime() : event.getEndTime();
        if (!start.isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian bắt đầu sự kiện phải ở trong tương lai");
        }
        if (!end.isAfter(start)) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription().trim());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            event.setCategory(category);
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            event.setLocation(request.getLocation().trim());
        }
        if (request.getEstimatedBudget() != null) {
            event.setEstimatedBudget(request.getEstimatedBudget());
        }
        if (request.getBannerUrl() != null) {
            event.setBannerUrl(request.getBannerUrl().trim());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        if (request.getHasSeatMap() != null) {
            boolean nowSeatMap = Boolean.TRUE.equals(request.getHasSeatMap());
            event.setHasSeatMap(nowSeatMap);
            if (nowSeatMap) {
                int r = request.getSeatRows() != null && request.getSeatRows() > 0 ? request.getSeatRows() : (event.getSeatRows() != null ? event.getSeatRows() : 0);
                int c = request.getSeatCols() != null && request.getSeatCols() > 0 ? request.getSeatCols() : (event.getSeatCols() != null ? event.getSeatCols() : 0);
                event.setSeatRows(r);
                event.setSeatCols(c);
                long existingSeats = seatRepository.countByEventId(eventId);
                if (existingSeats == 0 && r > 0 && c > 0) {
                    List<Seat> seats = new ArrayList<>();
                    for (int row = 1; row <= r; row++) {
                        char rowLetter = (char) ('A' + (row - 1));
                        for (int col = 1; col <= c; col++) {
                            String seatCode = rowLetter + String.valueOf(col);
                            String type = (row <= 2) ? "VIP" : "STANDARD";
                            Seat seat = Seat.builder()
                                    .event(event)
                                    .seatCode(seatCode)
                                    .seatRow(row)
                                    .seatCol(col)
                                    .seatType(type)
                                    .isBooked(false)
                                    .build();
                            seats.add(seat);
                        }
                    }
                    seatRepository.saveAll(seats);
                    event.setMaxAttendees(seats.size());
                }
            }
        }

        return toDto(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        deleteEvent(eventId, null);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long actorId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện với ID: " + eventId));
        // Internal account deletion may have loaded this entity from an older MySQL transaction snapshot.
        entityManager.refresh(event, LockModeType.PESSIMISTIC_WRITE);
        if (actorId != null) assertCanManage(event, actorId);
        assertCanModify(event);

        // 1. Xóa Check-in (phụ thuộc vào Registration và Event)
        entityManager.createNativeQuery(
            "DELETE FROM check_ins WHERE event_id = :id OR registration_id IN (SELECT id FROM registrations WHERE event_id = :id)"
        ).setParameter("id", eventId).executeUpdate();

        // 2. Xóa Đăng ký / Vé (phụ thuộc vào Seat và Event)
        entityManager.createNativeQuery(
            "DELETE FROM registrations WHERE event_id = :id"
        ).setParameter("id", eventId).executeUpdate();

        // 3. Xóa Chỗ ngồi (phụ thuộc vào Event)
        entityManager.createNativeQuery(
            "DELETE FROM seats WHERE event_id = :id"
        ).setParameter("id", eventId).executeUpdate();

        // 4. Xóa Đánh giá / Phản hồi (phụ thuộc vào Event)
        entityManager.createNativeQuery(
            "DELETE FROM feedbacks WHERE event_id = :id"
        ).setParameter("id", eventId).executeUpdate();

        // 5. Xóa Chi phí / Kinh phí (phụ thuộc vào Event)
        entityManager.createNativeQuery(
            "DELETE FROM expenses WHERE event_id = :id"
        ).setParameter("id", eventId).executeUpdate();

        // 6. Xóa chính Sự kiện
        entityManager.createNativeQuery(
            "DELETE FROM events WHERE id = :id"
        ).setParameter("id", eventId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    private void assertCanManage(Event event, Long actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy người dùng"));
        boolean admin = actor.getRole() != null && "ROLE_ADMIN".equals(actor.getRole().getName());
        if (!admin && !event.getOrganizer().getId().equals(actorId)) {
            throw new AccessDeniedException("Bạn chỉ có thể sửa hoặc xóa sự kiện do mình tổ chức");
        }
    }

    private String modificationBlockedReason(Event event) {
        if (event.getStatus() == EventStatus.ONGOING || event.getStatus() == EventStatus.COMPLETED
                || !event.getStartTime().isAfter(LocalDateTime.now())) {
            return "Không thể sửa hoặc xóa sự kiện đã bắt đầu hoặc đã kết thúc";
        }
        if (Boolean.TRUE.equals(event.getHasRegistrationHistory())
                || registrationRepository.existsByEventId(event.getId())) {
            return "Không thể sửa hoặc xóa sự kiện đã có người đăng ký, kể cả đăng ký đã hủy";
        }
        return null;
    }

    private void assertCanModify(Event event) {
        String reason = modificationBlockedReason(event);
        if (reason != null) throw new RuntimeException(reason);
    }

    public EventResponseDto toDto(Event event) {
        long confirmedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
        int remaining = Math.max(0, event.getMaxAttendees() - (int) confirmedCount);
        String blockedReason = modificationBlockedReason(event);

        Double avgRating = feedbackRepository.getAverageRatingByEventId(event.getId());
        List<Feedback> feedbacks = feedbackRepository.findByEventIdOrderByCreatedAtDesc(event.getId());

        String statusText = switch (event.getStatus()) {
            case DRAFT -> "Nháp";
            case APPROVED -> "Đã duyệt";
            case OPEN_FOR_REGISTRATION -> "Đang mở đăng ký";
            case FULL -> "Đã đủ chỗ";
            case ONGOING -> "Đang diễn ra";
            case COMPLETED -> "Đã kết thúc";
            case CANCELLED -> "Đã hủy";
        };

        return EventResponseDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFullName())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .maxAttendees(event.getMaxAttendees())
                .registeredAttendees((int) confirmedCount)
                .canModify(blockedReason == null)
                .modificationBlockedReason(blockedReason)
                .remainingSeats(remaining)
                .estimatedBudget(event.getEstimatedBudget())
                .status(event.getStatus())
                .statusText(statusText)
                .bannerUrl(event.getBannerUrl())
                .hasSeatMap(event.getHasSeatMap())
                .seatRows(event.getSeatRows())
                .seatCols(event.getSeatCols())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .feedbackCount(feedbacks.size())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
