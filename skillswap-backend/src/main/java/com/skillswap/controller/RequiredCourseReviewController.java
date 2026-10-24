package com.skillswap.controller;

import com.skillswap.dto.RequiredCourseReviewDTO;
import com.skillswap.service.SessionReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/class-sessions/reviews")
public class RequiredCourseReviewController {

    private final SessionReviewService reviewService;

    public RequiredCourseReviewController(SessionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/required")
    public ResponseEntity<List<RequiredCourseReviewDTO>> getRequiredReviews(
            Authentication authentication) {
        return ResponseEntity.ok(
                reviewService.getRequiredCourseReviews(authentication));
    }
}
