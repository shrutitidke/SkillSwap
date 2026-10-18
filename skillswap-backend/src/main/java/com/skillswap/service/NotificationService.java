package com.skillswap.service;

import com.skillswap.dto.NotificationResponseDTO;
import com.skillswap.entity.Notification;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.repository.NotificationRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponseDTO> getMyNotifications(
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<NotificationResponseDTO> getMyUnreadNotifications(
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public void markAsRead(
            Long notificationId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Notification not found"));

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    public void createNotification(
            User user,
            NotificationType type,
            String message) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    private NotificationResponseDTO toResponseDTO(
            Notification notification) {

        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());

        return dto;
    }
}