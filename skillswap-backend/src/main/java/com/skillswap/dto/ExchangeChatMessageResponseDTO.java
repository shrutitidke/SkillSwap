package com.skillswap.dto;

import java.time.LocalDateTime;

public record ExchangeChatMessageResponseDTO(
        Long id,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {
}
