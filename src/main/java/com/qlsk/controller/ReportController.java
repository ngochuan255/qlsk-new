package com.qlsk.controller;

import com.qlsk.dto.ReportDtos.*;
import com.qlsk.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/event/{eventId}/attendance")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<AttendanceReportDto> getAttendanceReport(@PathVariable Long eventId) {
        return ResponseEntity.ok(reportService.getAttendanceReport(eventId));
    }

    @GetMapping("/event/{eventId}/financial")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<FinancialReportDto> getFinancialReport(@PathVariable Long eventId) {
        return ResponseEntity.ok(reportService.getFinancialReport(eventId));
    }

    @GetMapping("/event/{eventId}/feedback")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<FeedbackSummaryDto> getFeedbackReport(@PathVariable Long eventId) {
        return ResponseEntity.ok(reportService.getFeedbackReport(eventId));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardSummaryDto> getAdminDashboard() {
        return ResponseEntity.ok(reportService.getAdminDashboardSummary());
    }

    @GetMapping("/admin/charts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminChartReportDto> getAdminCharts() {
        return ResponseEntity.ok(reportService.getAdminChartData());
    }
}
