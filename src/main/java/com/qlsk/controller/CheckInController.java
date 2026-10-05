package com.qlsk.controller;

import com.qlsk.dto.CheckInDtos.*;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponseDto> checkIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(checkInService.processCheckIn(principal.getId(), request));
    }
}
