package com.qlsk.service;

import com.qlsk.dto.FeedbackDtos.*;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final CheckInRepository checkInRepository;

    @Transactional
    public FeedbackResponseDto submitFeedback(Long userId, FeedbackRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Check if event has ended or completed
        if (LocalDateTime.now().isBefore(event.getEndTime()) && event.getStatus() != EventStatus.COMPLETED) {
            throw new RuntimeException("Chỉ có thể đánh giá sau khi sự kiện kết thúc!");
        }

        // Check if attendee registered
        Registration registration = registrationRepository.findByEventIdAndUserIdAndStatus(
                event.getId(), user.getId(), RegistrationStatus.CONFIRMED)
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký tham gia sự kiện này!"));

        // Check if attendee was checked in (tham dự thực tế)
        if (!checkInRepository.existsByRegistrationId(registration.getId())) {
            throw new RuntimeException("Chỉ người tham gia đã được ghi nhận Check-in thực tế mới có thể đánh giá sự kiện!");
        }

        // Check if already reviewed
        if (feedbackRepository.existsByEventIdAndUserId(event.getId(), user.getId())) {
            throw new RuntimeException("Bạn đã gửi đánh giá cho sự kiện này rồi!");
        }

        Feedback feedback = Feedback.builder()
                .event(event)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toDto(feedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponseDto> getEventFeedbacks(Long eventId) {
        return feedbackRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public FeedbackResponseDto toDto(Feedback fb) {
        return FeedbackResponseDto.builder()
                .id(fb.getId())
                .eventId(fb.getEvent().getId())
                .userId(fb.getUser().getId())
                .userName(fb.getUser().getFullName())
                .rating(fb.getRating())
                .comment(fb.getComment())
                .createdAt(fb.getCreatedAt())
                .build();
    }
}
