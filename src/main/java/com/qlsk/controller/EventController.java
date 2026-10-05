package com.qlsk.controller;

import com.qlsk.dto.EventDtos.*;
import com.qlsk.entity.EventStatus;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Public endpoints
    @GetMapping("/events/public")
    public ResponseEntity<List<EventResponseDto>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(eventService.searchEvents(keyword, categoryId, status, startDate, endDate));
    }

    @GetMapping("/events/public/{id}")
    public ResponseEntity<EventResponseDto> getEventDetail(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/events/public/{id}/seats")
    public ResponseEntity<List<SeatDto>> getEventSeats(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventSeats(id));
    }

    // Organizer endpoints
    @PostMapping("/organizer/events")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(eventService.createEvent(principal.getId(), request));
    }

    @GetMapping("/organizer/events/my")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<List<EventResponseDto>> getMyEvents(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(principal.getId()));
    }

    @PatchMapping("/organizer/events/{id}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        EventStatus newStatus = EventStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(eventService.updateEventStatus(id, newStatus));
    }

    // Admin endpoints
    @GetMapping("/admin/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventResponseDto>> getAllEventsForAdmin() {
        return ResponseEntity.ok(eventService.getAllEventsForAdmin());
    }

    @PatchMapping("/admin/events/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponseDto> approveEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.approveEvent(id, principal.getId()));
    }

    @PutMapping({"/admin/events/{id}", "/organizer/events/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<EventResponseDto> updateEvent(
            @PathVariable Long id,
            @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @DeleteMapping({"/admin/events/{id}", "/organizer/events/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sự kiện thành công"));
    }
}
