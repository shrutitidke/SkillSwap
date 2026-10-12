package com.skillswap.controller;

import com.skillswap.dto.ClassSessionCreateDTO;
import com.skillswap.dto.ClassSessionResponseDTO;
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
    public ResponseEntity<ClassSessionResponseDTO> proposeSession(
            @PathVariable Long requestId,
            @Valid @RequestBody ClassSessionCreateDTO dto,
            Authentication authentication) {

        ClassSessionResponseDTO response =
                sessionService.proposeSession(
                        requestId, dto, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<ClassSessionResponseDTO>> getMySessions(
            Authentication authentication) {

        return ResponseEntity.ok(
                sessionService.getMySessions(authentication)
        );
    }

    @PutMapping("/{sessionId}/respond")
    public ResponseEntity<ClassSessionResponseDTO> respondToSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody ClassSessionDecisionDTO dto,
            Authentication authentication) {

        ClassSessionResponseDTO response =
                sessionService.respondToSession(
                        sessionId, dto, authentication);

        return ResponseEntity.ok(response);
    }

}
