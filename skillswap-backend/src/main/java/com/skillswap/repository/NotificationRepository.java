package com.skillswap.repository;

import com.skillswap.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId);

    Optional<Notification> findByIdAndUserId(
            Long notificationId,
            Long userId
    );
}