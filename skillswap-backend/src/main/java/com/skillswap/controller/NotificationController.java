package com.skillswap.controller;

import com.skillswap.dto.NotificationResponseDTO;
import com.skillswap.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getMyNotifications(authentication));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getMyUnreadNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getMyUnreadNotifications(authentication));
    }

    @PutMapping("/read")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication) {
        notificationService.markAllAsRead(authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        notificationService.markAsRead(
                notificationId,
                authentication);

        return ResponseEntity.noContent().build();
    }
}