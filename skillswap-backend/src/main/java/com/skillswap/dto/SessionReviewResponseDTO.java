package com.skillswap.dto;

import java.time.LocalDateTime;

public record SessionReviewResponseDTO(
        Long id,
        Long sessionId,
        Long reviewerId,
        String reviewerName,
        Long revieweeId,
        String revieweeName,
        Integer rating,
        String comment,
        LocalDateTime createdAt) {
}
