package com.skillswap.service;

import com.skillswap.dto.SkillExchangeRequestCreateDTO;
import com.skillswap.dto.SkillExchangeRequestResponseDTO;
import com.skillswap.entity.Skill;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.Role;
import com.skillswap.enums.SkillType;
import com.skillswap.exception.SkillNotFoundException;
import com.skillswap.exception.UserNotFoundException;
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

        User learner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));
        if (learner.getRole() != Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only regular users can send exchange requests");
        }

        Skill teacherSkill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() ->
                        new SkillNotFoundException("Skill not found"));
        User teacher = teacherSkill.getUser();

        if (teacher.getRole() != Role.USER
                || teacher.getId().equals(learner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This member is not eligible for an exchange request");
        }
        if (teacherSkill.getType() != SkillType.TEACH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You can only request a skill offered for teaching");
        }

        List<Skill> learnerSkills = skillRepository.findByUserId(learner.getId());
        List<Skill> teacherSkills = skillRepository.findByUserId(teacher.getId());

        boolean learnerWantsTeacherSkill = learnerSkills.stream()
                .anyMatch(skill -> skill.getType() == SkillType.LEARN
                        && normalizedKey(skill.getName())
                                .equals(normalizedKey(teacherSkill.getName())));
        if (!learnerWantsTeacherSkill) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The requested skill is not a reciprocal match");
        }

        Skill learnerSkill = selectLearnerSkill(
                request.getLearnerSkillId(),
                learnerSkills,
                teacherSkills,
                learner.getId());

        if (requestRepository
                .existsByLearnerIdAndSkillIdAndLearnerSkillIdAndStatus(
                        learner.getId(),
                        teacherSkill.getId(),
                        learnerSkill.getId(),
                        RequestStatus.PENDING)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending request already exists for this skill exchange");
        }

        SkillExchangeRequest skillRequest = new SkillExchangeRequest();
        skillRequest.setSkill(teacherSkill);
        skillRequest.setLearnerSkill(learnerSkill);
        skillRequest.setLearner(learner);
        skillRequest.setTeacher(teacher);
        skillRequest.setStatus(RequestStatus.PENDING);
        skillRequest.setMessage(request.getMessage());

        SkillExchangeRequest savedRequest =
                requestRepository.save(skillRequest);

        notificationService.createNotification(
                teacher,
                NotificationType.SKILL_REQUEST_RECEIVED,
                learner.getName() + " has requested to learn "
                        + teacherSkill.getName()
        );

        return toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<SkillExchangeRequestResponseDTO> getReceivedRequests(
            Authentication authentication) {

        User teacher = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return requestRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacher.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillExchangeRequestResponseDTO> getSentRequests(
            Authentication authentication) {

        User learner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return requestRepository
                .findByLearnerIdOrderByCreatedAtDesc(learner.getId())
                .stream()
                .map(this::toResponse)
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

        User teacher = userRepository.findByEmail(authentication.getName())
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
        SkillExchangeRequest savedRequest = requestRepository.save(request);

        NotificationType notificationType =
                newStatus == RequestStatus.ACCEPTED
                        ? NotificationType.SKILL_REQUEST_ACCEPTED
                        : NotificationType.SKILL_REQUEST_REJECTED;
        String decision = newStatus == RequestStatus.ACCEPTED
                ? " accepted your request to learn "
                : " rejected your request to learn ";
        notificationService.createNotification(
                savedRequest.getLearner(),
                notificationType,
                teacher.getName() + decision
                        + savedRequest.getSkill().getName());

        return toResponse(savedRequest);
    }

    private Skill selectLearnerSkill(
            Long learnerSkillId,
            List<Skill> learnerSkills,
            List<Skill> teacherSkills,
            Long learnerId) {
        List<String> teacherLearnKeys = teacherSkills.stream()
                .filter(skill -> skill.getType() == SkillType.LEARN)
                .map(skill -> normalizedKey(skill.getName()))
                .toList();

        List<Skill> eligibleLearnerSkills = learnerSkills.stream()
                .filter(skill -> skill.getType() == SkillType.TEACH)
                .filter(skill -> teacherLearnKeys.contains(
                        normalizedKey(skill.getName())))
                .sorted(Comparator.comparing(Skill::getId))
                .toList();

        if (learnerSkillId == null) {
            return eligibleLearnerSkills.stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "You and this member do not have reciprocal skills"));
        }

        Skill selectedSkill = learnerSkills.stream()
                .filter(skill -> skill.getId().equals(learnerSkillId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The selected skill is not owned by you"));
        if (!eligibleLearnerSkills.contains(selectedSkill)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected skill is not part of this reciprocal match");
        }
        if (!selectedSkill.getUser().getId().equals(learnerId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected skill is not owned by you");
        }
        return selectedSkill;
    }

    private SkillExchangeRequestResponseDTO toResponse(
            SkillExchangeRequest request) {
        return new SkillExchangeRequestResponseDTO(
                request.getId(),
                request.getLearner().getName(),
                request.getTeacher().getName(),
                request.getMessage(),
                request.getSkill().getName(),
                request.getStatus(),
                request.getLearnerSkill() == null
                        ? null
                        : request.getLearnerSkill().getName());
    }

    private String normalizedKey(String value) {
        return value.trim().replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
