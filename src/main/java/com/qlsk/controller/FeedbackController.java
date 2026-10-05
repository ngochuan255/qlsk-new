package com.qlsk.controller;

import com.qlsk.dto.FeedbackDtos.*;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponseDto> submitFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.submitFeedback(principal.getId(), request));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<FeedbackResponseDto>> getEventFeedbacks(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackService.getEventFeedbacks(eventId));
    }
}
