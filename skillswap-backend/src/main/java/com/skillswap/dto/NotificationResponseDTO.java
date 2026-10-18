package com.skillswap.dto;

import com.skillswap.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDTO {

    private Long id;
    private NotificationType type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}