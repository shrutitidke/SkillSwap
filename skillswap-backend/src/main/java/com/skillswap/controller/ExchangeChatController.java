package com.skillswap.controller;

import com.skillswap.dto.ExchangeChatMessageCreateDTO;
import com.skillswap.dto.ExchangeChatMessageResponseDTO;
import com.skillswap.service.ExchangeChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skill-requests/{requestId}/messages")
public class ExchangeChatController {

    private final ExchangeChatService chatService;

    public ExchangeChatController(ExchangeChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<List<ExchangeChatMessageResponseDTO>> getMessages(
            @PathVariable Long requestId,
            @RequestParam(required = false) Long afterId,
            Authentication authentication) {
        return ResponseEntity.ok(
                chatService.getMessages(requestId, afterId, authentication));
    }

    @PostMapping
    public ResponseEntity<ExchangeChatMessageResponseDTO> sendMessage(
            @PathVariable Long requestId,
            @Valid @RequestBody ExchangeChatMessageCreateDTO message,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(
                        requestId, message, authentication));
    }
}
