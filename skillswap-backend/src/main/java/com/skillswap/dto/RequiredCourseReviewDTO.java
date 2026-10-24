package com.skillswap.dto;

import java.time.LocalDateTime;

public record RequiredCourseReviewDTO(
        Long sessionId,
        Long requestId,
        Long teacherId,
        String teacherName,
        String skillName,
        LocalDateTime scheduledAt,
        Integer durationMinutes) {
}
