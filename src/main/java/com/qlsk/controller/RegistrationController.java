package com.qlsk.controller;

import com.qlsk.dto.RegistrationDtos.*;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/registrations")
    public ResponseEntity<TicketDto> registerEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterEventRequest request) {
        return ResponseEntity.ok(registrationService.registerEvent(principal.getId(), request));
    }

    @GetMapping("/registrations/my-tickets")
    public ResponseEntity<List<TicketDto>> getMyTickets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(registrationService.getUserTickets(principal.getId()));
    }

    @PostMapping("/registrations/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelRegistration(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        registrationService.cancelRegistration(principal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Hủy đăng ký thành công"));
    }

    @GetMapping("/organizer/events/{eventId}/registrations")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<TicketDto>> getEventRegistrations(@PathVariable Long eventId,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(registrationService.getOrganizerEventRegistrations(principal.getId(), eventId));
    }

    @GetMapping("/admin/events/{eventId}/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketDto>> getAdminEventRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getEventRegistrations(eventId));
    }

    @PostMapping("/admin/registrations/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminCancelRegistration(@PathVariable Long id) {
        registrationService.adminCancelRegistration(id);
        return ResponseEntity.ok(Map.of("message", "Hủy đăng ký vé thành công"));
    }
}
