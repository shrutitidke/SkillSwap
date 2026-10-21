package com.skillswap.controller;

import com.skillswap.dto.PublicUserProfileDTO;
import com.skillswap.service.SessionReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/users")
public class PublicProfileController {

    private final SessionReviewService reviewService;

    public PublicProfileController(SessionReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<PublicUserProfileDTO> getPublicProfile(
            @PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getPublicProfile(userId));
    }
}
