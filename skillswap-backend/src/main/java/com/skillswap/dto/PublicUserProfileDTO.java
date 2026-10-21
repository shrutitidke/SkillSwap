package com.skillswap.dto;

import java.util.List;

public record PublicUserProfileDTO(
        Long id,
        String name,
        double averageRating,
        long reviewCount,
        List<PublicReviewDTO> reviews) {
}
