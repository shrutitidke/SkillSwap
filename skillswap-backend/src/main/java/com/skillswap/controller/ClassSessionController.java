package com.skillswap.controller;

import com.skillswap.dto.ClassSessionCreateDTO;
import com.skillswap.dto.ClassSessionResponseDTO;
import com.skillswap.enums.SessionStatus;
import com.skillswap.service.ClassSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.skillswap.dto.ClassSessionDecisionDTO;

@RestController
@RequestMapping("/api/class-sessions")
public class ClassSessionController {

    private final ClassSessionService sessionService;

    public ClassSessionController(ClassSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/requests/{requestId}")
    public ResponseEntity<String> proposeSession(
            @PathVariable Long requestId,
            @Valid @RequestBody ClassSessionCreateDTO dto,
            Authentication authentication) {

        sessionService.proposeSession(
                requestId, dto, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Class session proposed successfully");
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<ClassSessionResponseDTO>> getMySessions(
            Authentication authentication) {

        return ResponseEntity.ok(
                sessionService.getMySessions(authentication)
        );
    }

    @PutMapping("/{sessionId}/respond")
    public ResponseEntity<String> respondToSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody ClassSessionDecisionDTO dto,
            Authentication authentication) {

        sessionService.respondToSession(
                sessionId, dto, authentication);

        String message;

        if (dto.getStatus() == SessionStatus.CONFIRMED) {
            message = "Class session confirmed successfully";
        } else {
            message = "Class session declined successfully";
        }

        return ResponseEntity.ok(message);
    }

    @PutMapping("/{sessionId}/cancel")
    public ResponseEntity<String> cancelSession(
            @PathVariable Long sessionId,
            Authentication authentication) {

        sessionService.cancelSession(sessionId, authentication);

        return ResponseEntity.ok("Class session cancelled successfully");
    }

}
