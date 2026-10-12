package com.skillswap.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClassSessionCreateDTO {

    @NotNull(message = "Scheduled date and time are required")
    @Future(message = "Scheduled date and time must be in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 240, message = "Duration cannot exceed 240 minutes")
    private Integer durationMinutes;

    @Size(max = 1000, message = "Meeting link cannot exceed 1000 characters")
    private String meetingLink;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
