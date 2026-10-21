package com.skillswap.dto;

import java.time.LocalDateTime;

public record PublicReviewDTO(
        Long id,
        String reviewerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt) {
}
