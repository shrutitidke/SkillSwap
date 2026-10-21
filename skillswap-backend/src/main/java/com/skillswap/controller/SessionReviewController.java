package com.skillswap.controller;

import com.skillswap.dto.SessionReviewCreateDTO;
import com.skillswap.dto.SessionReviewResponseDTO;
import com.skillswap.service.SessionReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-sessions/{sessionId}/reviews")
public class SessionReviewController {

    private final SessionReviewService reviewService;

    public SessionReviewController(SessionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<SessionReviewResponseDTO>> getSessionReviews(
            @PathVariable Long sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(
                reviewService.getSessionReviews(sessionId, authentication));
    }

    @PostMapping
    public ResponseEntity<SessionReviewResponseDTO> createReview(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionReviewCreateDTO request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reviewService.createReview(
                        sessionId, request, authentication));
    }
}
