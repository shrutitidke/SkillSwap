package com.skillswap.service;

import com.skillswap.dto.AdminMessageCreateDTO;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.Role;
import com.skillswap.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private SkillExchangeRequestRepository requestRepository;
    @Mock
    private ClassSessionRepository sessionRepository;
    @Mock
    private ExchangeChatMessageRepository messageRepository;
    @Mock
    private SessionReviewRepository reviewRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PasswordResetChallengeRepository resetChallengeRepository;
    @Mock
    private NotificationService notificationService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                skillRepository,
                requestRepository,
                sessionRepository,
                messageRepository,
                reviewRepository,
                notificationRepository,
                resetChallengeRepository,
                notificationService);
    }

    @Test
    void administratorAccountsCannotBeDeleted() {
        User administrator = user(7L, Role.ADMIN);
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(administrator));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.deleteUser(7L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(requestRepository, never())
                .findByLearnerIdOrTeacherId(7L, 7L);
        verify(userRepository, never()).delete(administrator);
    }

    @Test
    void deletesUserRecordsInForeignKeySafeOrder() {
        User member = user(8L, Role.USER);
        when(userRepository.findById(8L)).thenReturn(Optional.of(member));
        when(requestRepository.findByLearnerIdOrTeacherId(8L, 8L))
                .thenReturn(java.util.List.of());

        adminService.deleteUser(8L);

        InOrder order = inOrder(
                reviewRepository,
                notificationRepository,
                resetChallengeRepository,
                skillRepository,
                userRepository);
        order.verify(reviewRepository)
                .deleteByReviewerIdOrRevieweeId(8L, 8L);
        order.verify(notificationRepository).deleteByUserId(8L);
        order.verify(resetChallengeRepository).deleteByUserId(8L);
        order.verify(skillRepository).deleteByUserId(8L);
        order.verify(userRepository).delete(member);
    }

    @Test
    void adminCanSendInstructionToOneRegularUser() {
        User member = user(8L, Role.USER);
        when(userRepository.findById(8L)).thenReturn(Optional.of(member));

        int recipients = adminService.sendAdminMessage(
                new AdminMessageCreateDTO(
                        NotificationType.ADMIN_INSTRUCTION,
                        8L,
                        "  Please update your profile.  "));

        assertEquals(1, recipients);
        verify(notificationService).createNotification(
                member,
                NotificationType.ADMIN_INSTRUCTION,
                "Please update your profile.");
    }

    @Test
    void adminCanBroadcastWarningToAllRegularUsers() {
        User firstMember = user(8L, Role.USER);
        User secondMember = user(9L, Role.USER);
        when(userRepository.findAllByRole(Role.USER))
                .thenReturn(java.util.List.of(firstMember, secondMember));

        int recipients = adminService.sendAdminMessage(
                new AdminMessageCreateDTO(
                        NotificationType.ADMIN_WARNING,
                        null,
                        "Follow the community guidelines."));

        assertEquals(2, recipients);
        verify(notificationService).createNotification(
                firstMember,
                NotificationType.ADMIN_WARNING,
                "Follow the community guidelines.");
        verify(notificationService).createNotification(
                secondMember,
                NotificationType.ADMIN_WARNING,
                "Follow the community guidelines.");
    }

    @Test
    void adminMessageCannotTargetAdminAccount() {
        User administrator = user(7L, Role.ADMIN);
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(administrator));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.sendAdminMessage(
                        new AdminMessageCreateDTO(
                                NotificationType.ADMIN_WARNING,
                                7L,
                                "Message")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(notificationService, never())
                .createNotification(any(), any(), anyString());
    }

    @Test
    void adminCannotSendOtherNotificationTypesThroughMessageTool() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminService.sendAdminMessage(
                        new AdminMessageCreateDTO(
                                NotificationType.SESSION_CANCELLED,
                                null,
                                "Message")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(role == Role.ADMIN ? "Admin" : "Member");
        user.setEmail(role == Role.ADMIN
                ? "admin@example.test"
                : "member@example.test");
        user.setRole(role);
        return user;
    }
}
