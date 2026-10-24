package com.skillswap.service;

import com.skillswap.dto.AdminSkillDTO;
import com.skillswap.dto.AdminUserDTO;
import com.skillswap.dto.AdminMessageCreateDTO;
import com.skillswap.entity.Skill;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.Role;
import com.skillswap.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final SkillExchangeRequestRepository requestRepository;
    private final ClassSessionRepository sessionRepository;
    private final ExchangeChatMessageRepository messageRepository;
    private final SessionReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetChallengeRepository resetChallengeRepository;
    private final NotificationService notificationService;

    public AdminService(
            UserRepository userRepository,
            SkillRepository skillRepository,
            SkillExchangeRequestRepository requestRepository,
            ClassSessionRepository sessionRepository,
            ExchangeChatMessageRepository messageRepository,
            SessionReviewRepository reviewRepository,
            NotificationRepository notificationRepository,
            PasswordResetChallengeRepository resetChallengeRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.requestRepository = requestRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.resetChallengeRepository = resetChallengeRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDTO> getUsers() {
        List<User> users = userRepository.findAll();
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, List<AdminSkillDTO>> skillsByUser = userIds.isEmpty()
                ? Map.of()
                : skillRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.groupingBy(
                                skill -> skill.getUser().getId(),
                                Collectors.mapping(this::toSkillDTO,
                                        Collectors.toList())));

        return users.stream()
                .map(user -> new AdminUserDTO(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt(),
                        skillsByUser.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be deleted here");
        }

        var requests = requestRepository.findByLearnerIdOrTeacherId(
                userId, userId);
        List<Long> requestIds = requests.stream()
                .map(request -> request.getId())
                .toList();

        reviewRepository.deleteByReviewerIdOrRevieweeId(userId, userId);
        if (!requestIds.isEmpty()) {
            messageRepository.deleteBySkillExchangeRequestIdIn(requestIds);
            reviewRepository.deleteBySessionSkillExchangeRequestIdIn(
                    requestIds);
            sessionRepository.deleteBySkillExchangeRequestIdIn(requestIds);
            requestRepository.deleteAllInBatch(requests);
        }
        notificationRepository.deleteByUserId(userId);
        resetChallengeRepository.deleteByUserId(userId);
        skillRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    @Transactional
    public int sendAdminMessage(AdminMessageCreateDTO request) {
        if (request.type() != NotificationType.ADMIN_WARNING
                && request.type() != NotificationType.ADMIN_INSTRUCTION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message type must be an admin warning or instruction");
        }
        String message = request.message().trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty");
        }

        List<User> recipients;
        if (request.userId() == null) {
            recipients = userRepository.findAllByRole(Role.USER);
        } else {
            User recipient = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"));
            if (recipient.getRole() != Role.USER) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Admin messages can only be sent to regular users");
            }
            recipients = List.of(recipient);
        }

        for (User recipient : recipients) {
            notificationService.createNotification(
                    recipient,
                    request.type(),
                    message);
        }
        return recipients.size();
    }

    private AdminSkillDTO toSkillDTO(Skill skill) {
        return new AdminSkillDTO(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getType());
    }
}
