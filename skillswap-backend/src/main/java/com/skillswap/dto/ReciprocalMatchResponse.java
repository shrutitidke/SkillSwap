package com.skillswap.dto;

public record ReciprocalMatchResponse(
        Long userId,
        String userName,
        Long skillTheyTeachId,
        Long skillYouTeachId,
        String skillTheyTeach,
        String skillTheyTeachDescription,
        String skillYouTeach,
        String skillYouTeachDescription,
        String explanation
) {
}
