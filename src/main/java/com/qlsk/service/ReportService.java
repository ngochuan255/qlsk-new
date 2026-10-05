package com.qlsk.service;

import com.qlsk.dto.ExpenseDtos;
import com.qlsk.dto.FeedbackDtos;
import com.qlsk.dto.ReportDtos.*;
import com.qlsk.entity.*;
import com.qlsk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final CheckInRepository checkInRepository;
    private final ExpenseRepository expenseRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ExpenseService expenseService;
    private final FeedbackService feedbackService;

    @Transactional(readOnly = true)
    public AttendanceReportDto getAttendanceReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        int totalSeats = event.getMaxAttendees();
        int registered = (int) registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        int checkedIn = (int) checkInRepository.countByEventId(eventId);
        int absent = Math.max(0, registered - checkedIn);

        double attendanceRate = registered > 0 ? Math.round(((double) checkedIn / registered * 100.0) * 10.0) / 10.0 : 0.0;
        double absenceRate = registered > 0 ? Math.round(((double) absent / registered * 100.0) * 10.0) / 10.0 : 0.0;

        return AttendanceReportDto.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalSeats(totalSeats)
                .totalRegistered(registered)
                .totalCheckedIn(checkedIn)
                .totalAbsent(absent)
                .attendanceRate(attendanceRate)
                .absenceRate(absenceRate)
                .build();
    }

    @Transactional(readOnly = true)
    public FinancialReportDto getFinancialReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        BigDecimal estimated = event.getEstimatedBudget() != null ? event.getEstimatedBudget() : BigDecimal.ZERO;
        BigDecimal used = expenseRepository.sumAmountByEventId(eventId);
        if (used == null) used = BigDecimal.ZERO;

        BigDecimal remaining = estimated.subtract(used);
        double usedRate = estimated.compareTo(BigDecimal.ZERO) > 0 
                ? used.divide(estimated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).doubleValue()
                : 0.0;

        List<ExpenseDtos.ExpenseResponseDto> expenses = expenseService.getEventExpenses(eventId);

        return FinancialReportDto.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .estimatedBudget(estimated)
                .totalUsedBudget(used)
                .remainingBudget(remaining)
                .budgetUsedRate(Math.round(usedRate * 10.0) / 10.0)
                .expenses(expenses)
                .build();
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryDto getFeedbackReport(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại"));

        List<FeedbackDtos.FeedbackResponseDto> feedbacks = feedbackService.getEventFeedbacks(eventId);
        Double avg = feedbackRepository.getAverageRatingByEventId(eventId);

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);

        List<Object[]> rawDist = feedbackRepository.getRatingDistributionByEventId(eventId);
        for (Object[] row : rawDist) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(rating, count);
        }

        return FeedbackSummaryDto.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalReviews(feedbacks.size())
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .ratingDistribution(distribution)
                .recentReviews(feedbacks.stream().limit(10).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getAdminDashboardSummary() {
        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.ONGOING)
                .count();
        long totalAttendees = userRepository.findAll().stream()
                .filter(u -> u.getRole().getName().equals("ROLE_ATTENDEE"))
                .count();
        long totalRegistrations = registrationRepository.countByStatus(RegistrationStatus.CONFIRMED);
        long totalCheckedIn = checkInRepository.count();

        BigDecimal totalExpenses = expenseRepository.findAll().stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryDto.builder()
                .totalEvents(totalEvents)
                .activeEvents(activeEvents)
                .totalAttendees(totalAttendees)
                .totalRegistrations(totalRegistrations)
                .totalCheckedIn(totalCheckedIn)
                .totalExpenses(totalExpenses)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChartReportDto getAdminChartData() {
        List<Event> allEvents = eventRepository.findAll();

        // 1. Events by status
        Map<String, Long> statusMap = new HashMap<>();
        for (EventStatus status : EventStatus.values()) {
            statusMap.put(status.name(), 0L);
        }
        for (Event e : allEvents) {
            statusMap.put(e.getStatus().name(), statusMap.getOrDefault(e.getStatus().name(), 0L) + 1);
        }

        // 2. Events by category
        Map<String, Long> catMap = new HashMap<>();
        for (Event e : allEvents) {
            String catName = e.getCategory() != null ? e.getCategory().getName() : "Khác";
            catMap.put(catName, catMap.getOrDefault(catName, 0L) + 1);
        }

        // 3. Top events attendance
        List<String> titles = new ArrayList<>();
        List<Integer> registrations = new ArrayList<>();
        List<Integer> checkIns = new ArrayList<>();
        for (Event e : allEvents.stream().limit(8).collect(Collectors.toList())) {
            titles.add(e.getTitle().length() > 20 ? e.getTitle().substring(0, 18) + "..." : e.getTitle());
            int regCount = (int) registrationRepository.countByEventIdAndStatus(e.getId(), RegistrationStatus.CONFIRMED);
            int ciCount = (int) checkInRepository.countByEventId(e.getId());
            registrations.add(regCount);
            checkIns.add(ciCount);
        }

        // 4. Budget vs Expenses
        BigDecimal totalEstimated = allEvents.stream()
                .map(e -> e.getEstimatedBudget() != null ? e.getEstimatedBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenseRepository.findAll().stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Rating distribution
        Map<Integer, Long> ratingDist = new HashMap<>();
        for (int i = 1; i <= 5; i++) ratingDist.put(i, 0L);
        for (Feedback fb : feedbackRepository.findAll()) {
            int r = fb.getRating();
            ratingDist.put(r, ratingDist.getOrDefault(r, 0L) + 1);
        }

        return AdminChartReportDto.builder()
                .eventsByStatus(statusMap)
                .eventsByCategory(catMap)
                .eventTitles(titles)
                .eventRegistrations(registrations)
                .eventCheckIns(checkIns)
                .totalEstimatedBudget(totalEstimated)
                .totalUsedExpenses(totalExpenses)
                .ratingDistribution(ratingDist)
                .build();
    }
}
