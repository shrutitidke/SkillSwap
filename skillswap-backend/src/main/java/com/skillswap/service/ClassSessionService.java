package com.skillswap.service;

import com.skillswap.dto.ClassSessionCreateDTO;
import com.skillswap.dto.ClassSessionDecisionDTO;
import com.skillswap.dto.ClassSessionResponseDTO;
import com.skillswap.entity.ClassSession;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.Skill;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.SessionStatus;
import com.skillswap.exception.UserNotFoundException;
import com.skillswap.repository.ClassSessionRepository;
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.UserRepository;
import com.skillswap.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassSessionService {

    private final ClassSessionRepository sessionRepository;
    private final SkillExchangeRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ClassSessionService(
            ClassSessionRepository sessionRepository,
            SkillExchangeRequestRepository requestRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.sessionRepository = sessionRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }


    @Transactional
    public ClassSessionResponseDTO proposeSession(
            Long requestId,
            ClassSessionCreateDTO dto,
            Authentication authentication) {

        String email = authentication.getName();

        User proposer = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        SkillExchangeRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Skill request not found"));

        boolean proposerIsTeacher =
                request.getTeacher().getId().equals(proposer.getId());
        boolean proposerIsLearner =
                request.getLearner().getId().equals(proposer.getId());
        if (!proposerIsTeacher && !proposerIsLearner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only exchange participants can propose a class");
        }

        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A class can only be proposed for an accepted request");
        }

        if (proposerIsLearner && request.getLearnerSkill() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A reciprocal teaching skill is required to propose a class");
        }

        ClassSession session = new ClassSession();
        session.setSkillExchangeRequest(request);
        session.setProposer(proposer);
        session.setScheduledAt(dto.getScheduledAt());
        session.setDurationMinutes(dto.getDurationMinutes());
        session.setMeetingLink(dto.getMeetingLink());
        session.setDescription(dto.getDescription());
        session.setStatus(SessionStatus.PROPOSED);

        ClassSession saved = sessionRepository.save(session);

        User otherParticipant =
                proposerIsTeacher ? request.getLearner() : request.getTeacher();
        notificationService.createNotification(
                otherParticipant,
                NotificationType.SESSION_PROPOSED,
                proposer.getName()
                        + " proposed a class for "
                        + teachingSkillName(session, request)
        );

        return toResponseDTO(saved);
    }

    private ClassSessionResponseDTO toResponseDTO(
            ClassSession session) {

        SkillExchangeRequest request =
                session.getSkillExchangeRequest();
        User proposer = sessionProposer(session, request);

        return new ClassSessionResponseDTO(
                session.getId(),
                request.getId(),
                request.getLearner().getId(),
                request.getTeacher().getId(),
                proposer.getId(),
                proposer.getName(),
                teachingSkillName(session, request),
                request.getLearner().getName(),
                request.getTeacher().getName(),
                request.getSkill().getName(),
                session.getScheduledAt(),
                session.getDurationMinutes(),
                session.getMeetingLink(),
                session.getDescription(),
                session.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponseDTO> getMySessions(
            Authentication authentication) {

        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        List<ClassSession> sessions =
                sessionRepository
                        .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                                currentUser.getId(), currentUser.getId());

        return sessions.stream()
                .map(this::toResponseDTO)
                .toList();
    }


    @Transactional
    public ClassSessionResponseDTO respondToSession(
            Long sessionId,
            ClassSessionDecisionDTO dto,
            Authentication authentication) {

        String email = authentication.getName();

        User responder = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Class session not found"));

        SkillExchangeRequest request =
                session.getSkillExchangeRequest();

        boolean isParticipant =
                request.getLearner().getId().equals(responder.getId())
                        || request.getTeacher().getId().equals(responder.getId());
        if (!isParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only exchange participants can respond to this session");
        }

        User proposer = sessionProposer(session, request);
        if (proposer.getId().equals(responder.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The other participant must respond to this session");
        }

        if (session.getStatus() != SessionStatus.PROPOSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only proposed sessions can be confirmed or declined");
        }

        SessionStatus newStatus = dto.getStatus();

        if (newStatus != SessionStatus.CONFIRMED
                && newStatus != SessionStatus.DECLINED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be CONFIRMED or DECLINED");
        }

        session.setStatus(newStatus);

        ClassSession saved = sessionRepository.save(session);

        NotificationType notificationType;
        String notificationMessage;

        if (newStatus == SessionStatus.CONFIRMED) {
            notificationType = NotificationType.SESSION_CONFIRMED;
            notificationMessage = responder.getName()
                    + " confirmed the class to teach "
                    + teachingSkillName(session, request);
        } else {
            notificationType = NotificationType.SESSION_DECLINED;
            notificationMessage = responder.getName()
                    + " declined the class to teach "
                    + teachingSkillName(session, request);
        }

        notificationService.createNotification(
                proposer,
                notificationType,
                notificationMessage
        );

        return toResponseDTO(saved);
    }

    @Transactional
    public ClassSessionResponseDTO cancelSession(
            Long sessionId,
            Authentication authentication) {

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        SkillExchangeRequest request = session.getSkillExchangeRequest();

        boolean isTeacher = request.getTeacher().getId()
                .equals(currentUser.getId());

        boolean isLearner = request.getLearner().getId()
                .equals(currentUser.getId());

        if (!isTeacher && !isLearner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to cancel this session");
        }

        if (session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This session cannot be cancelled");
        }

        if (!session.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sessions cannot be cancelled at or after their scheduled start time");
        }

        session.setStatus(SessionStatus.CANCELLED);

        ClassSession savedSession = sessionRepository.save(session);

        User otherParticipant = isTeacher
                ? request.getLearner()
                : request.getTeacher();

        notificationService.createNotification(
                otherParticipant,
                NotificationType.SESSION_CANCELLED,
                currentUser.getName()
                        + " cancelled the class to teach "
                        + teachingSkillName(session, request)
        );

        return toResponseDTO(savedSession);
    }

    private User sessionProposer(
            ClassSession session,
            SkillExchangeRequest request) {
        return session.getProposer() == null
                ? request.getTeacher()
                : session.getProposer();
    }

    private String teachingSkillName(
            ClassSession session,
            SkillExchangeRequest request) {
        User proposer = sessionProposer(session, request);
        Skill teachingSkill =
                proposer.getId().equals(request.getLearner().getId())
                        ? request.getLearnerSkill()
                        : request.getSkill();
        return teachingSkill == null
                ? request.getSkill().getName()
                : teachingSkill.getName();
    }


}
