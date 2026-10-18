package com.skillswap.service;

import com.skillswap.dto.SkillExchangeRequestCreateDTO;
import com.skillswap.dto.SkillExchangeRequestResponseDTO;
import com.skillswap.entity.Skill;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.SkillType;
import com.skillswap.exception.SkillNotFoundException;
import com.skillswap.exception.UserNotFoundException;
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SkillExchangeRequestService {

    private final SkillExchangeRequestRepository requestRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SkillExchangeRequestService(
            SkillExchangeRequestRepository requestRepository,
            SkillRepository skillRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.requestRepository = requestRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SkillExchangeRequestResponseDTO sendRequest(
            SkillExchangeRequestCreateDTO request,
            Authentication authentication) {

        String email = authentication.getName();

        User learner = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() ->
                        new SkillNotFoundException("Skill not found"));

        if (skill.getType() != SkillType.TEACH) {
            throw new IllegalArgumentException(
                    "You can only request skills that are offered for teaching");
        }

        if (skill.getUser().getId().equals(learner.getId())) {
            throw new IllegalArgumentException(
                    "You cannot request your own skill");
        }

        if (requestRepository.existsByLearnerIdAndSkillId(
                learner.getId(), skill.getId())) {
            throw new IllegalArgumentException(
                    "You have already requested this skill");
        }

        SkillExchangeRequest skillRequest =
                new SkillExchangeRequest();

        skillRequest.setSkill(skill);
        skillRequest.setLearner(learner);
        skillRequest.setTeacher(skill.getUser());
        skillRequest.setStatus(RequestStatus.PENDING);
        skillRequest.setMessage(request.getMessage());

        SkillExchangeRequest savedRequest =
                requestRepository.save(skillRequest);

        notificationService.createNotification(
                savedRequest.getTeacher(),
                NotificationType.SKILL_REQUEST_RECEIVED,
                learner.getName() + " has requested to learn "
                        + skill.getName()
        );

        return new SkillExchangeRequestResponseDTO(
                savedRequest.getId(),
                learner.getName(),
                skill.getUser().getName(),
                savedRequest.getMessage(),
                skill.getName(),
                savedRequest.getStatus()
        );

    }

    @Transactional(readOnly = true)
    public List<SkillExchangeRequestResponseDTO> getReceivedRequests(
            Authentication authentication) {

        String email = authentication.getName();

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        List<SkillExchangeRequest> requests =
                requestRepository.findByTeacherIdOrderByCreatedAtDesc(
                        teacher.getId());

        return requests.stream()
                .map(request -> new SkillExchangeRequestResponseDTO(
                        request.getId(),
                        request.getLearner().getName(),
                        request.getTeacher().getName(),
                        request.getMessage(),
                        request.getSkill().getName(),
                        request.getStatus()
                ))
                .toList();
    }

    @Transactional
    public SkillExchangeRequestResponseDTO respondToRequest(
            Long requestId,
            RequestStatus newStatus,
            Authentication authentication) {

        if (newStatus != RequestStatus.ACCEPTED
                && newStatus != RequestStatus.REJECTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACCEPTED or REJECTED");
        }

        String email = authentication.getName();

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        SkillExchangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Skill request not found"));

        if (!request.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to respond to this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This request has already been processed");
        }

        request.setStatus(newStatus);

        SkillExchangeRequest savedRequest =
                requestRepository.save(request);

        NotificationType notificationType;
        String notificationMessage;

        if (newStatus == RequestStatus.ACCEPTED) {
            notificationType = NotificationType.SKILL_REQUEST_ACCEPTED;
            notificationMessage = teacher.getName()
                    + " accepted your request to learn "
                    + savedRequest.getSkill().getName();
        } else {
            notificationType = NotificationType.SKILL_REQUEST_REJECTED;
            notificationMessage = teacher.getName()
                    + " rejected your request to learn "
                    + savedRequest.getSkill().getName();
        }

        notificationService.createNotification(
                savedRequest.getLearner(),
                notificationType,
                notificationMessage
        );

        return new SkillExchangeRequestResponseDTO(
                savedRequest.getId(),
                savedRequest.getLearner().getName(),
                savedRequest.getTeacher().getName(),
                savedRequest.getMessage(),
                savedRequest.getSkill().getName(),
                savedRequest.getStatus()
        );
    }


}
