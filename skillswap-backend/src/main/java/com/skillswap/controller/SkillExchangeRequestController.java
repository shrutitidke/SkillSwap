package com.skillswap.controller;

import com.skillswap.dto.SkillExchangeRequestCreateDTO;
import com.skillswap.dto.SkillExchangeRequestDecisionDTO;
import com.skillswap.dto.SkillExchangeRequestResponseDTO;
import com.skillswap.service.SkillExchangeRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/skill-requests")
public class SkillExchangeRequestController {

    private final SkillExchangeRequestService requestService;

    public SkillExchangeRequestController(
            SkillExchangeRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<SkillExchangeRequestResponseDTO> sendRequest(
            @Valid @RequestBody SkillExchangeRequestCreateDTO request,
            Authentication authentication) {

        SkillExchangeRequestResponseDTO response =
                requestService.sendRequest(request, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/received")
    public ResponseEntity<List<SkillExchangeRequestResponseDTO>> getReceivedRequests(
            Authentication authentication) {

        List<SkillExchangeRequestResponseDTO> requests =
                requestService.getReceivedRequests(authentication);

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<SkillExchangeRequestResponseDTO>> getSentRequests(
            Authentication authentication) {

        return ResponseEntity.ok(
                requestService.getSentRequests(authentication));
    }

    @PutMapping("/{requestId}/respond")
    public ResponseEntity<SkillExchangeRequestResponseDTO> respondToRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody SkillExchangeRequestDecisionDTO decision,
            Authentication authentication) {

        SkillExchangeRequestResponseDTO response =
                requestService.respondToRequest(
                        requestId,
                        decision.getStatus(),
                        authentication);

        return ResponseEntity.ok(response);
    }


}
