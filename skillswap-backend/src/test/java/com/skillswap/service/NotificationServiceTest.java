package com.skillswap.service;

import com.skillswap.entity.Notification;
import com.skillswap.entity.User;
import com.skillswap.repository.NotificationRepository;
import com.skillswap.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository =
            mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService =
            new NotificationService(notificationRepository, userRepository);

    @Test
    void marksAllCurrentUsersUnreadNotificationsAsRead() {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(12L);
        Notification unreadNotification = new Notification();
        when(authentication.getName()).thenReturn("member@example.test");
        when(userRepository.findByEmail("member@example.test"))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findByUserIdAndReadFalse(12L))
                .thenReturn(List.of(unreadNotification));

        notificationService.markAllAsRead(authentication);

        assertTrue(unreadNotification.isRead());
        verify(notificationRepository).saveAll(List.of(unreadNotification));
    }
}
