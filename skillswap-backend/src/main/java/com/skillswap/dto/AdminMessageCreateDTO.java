package com.skillswap.dto;

import com.skillswap.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminMessageCreateDTO(
        @NotNull NotificationType type,
        Long userId,
        @NotBlank @Size(max = 500) String message) {
}
