package com.skillswap.service;

import com.skillswap.dto.ClassSessionCreateDTO;
import com.skillswap.dto.ClassSessionDecisionDTO;
import com.skillswap.dto.ClassSessionResponseDTO;
import com.skillswap.entity.ClassSession;
import com.skillswap.entity.Skill;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.NotificationType;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.Role;
import com.skillswap.enums.SessionStatus;
import com.skillswap.enums.SkillType;
import com.skillswap.repository.ClassSessionRepository;
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {

    @Mock
    private ClassSessionRepository sessionRepository;

    @Mock
    private SkillExchangeRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    private ClassSessionService sessionService;
    private User learner;
    private User teacher;
    private User outsider;
    private Skill learnerSkill;
    private Skill teacherSkill;
    private SkillExchangeRequest acceptedRequest;

    @BeforeEach
    void setUp() {
        sessionService = new ClassSessionService(
                sessionRepository,
                requestRepository,
                userRepository,
                notificationService);
        learner = user(1L, "Payal", "payal@example.test");
        teacher = user(2L, "Shriya", "shriya@example.test");
        outsider = user(3L, "Outsider", "outsider@example.test");
        learnerSkill = skill(11L, learner, "Java", SkillType.TEACH);
        teacherSkill = skill(21L, teacher, "JavaScript", SkillType.TEACH);

        acceptedRequest = new SkillExchangeRequest();
        acceptedRequest.setId(17L);
        acceptedRequest.setLearner(learner);
        acceptedRequest.setTeacher(teacher);
        acceptedRequest.setLearnerSkill(learnerSkill);
        acceptedRequest.setSkill(teacherSkill);
        acceptedRequest.setStatus(RequestStatus.ACCEPTED);

        lenient().when(requestRepository.findById(17L))
                .thenReturn(Optional.of(acceptedRequest));
        lenient().when(sessionRepository.save(any(ClassSession.class)))
                .thenAnswer(invocation -> {
                    ClassSession session = invocation.getArgument(0);
                    session.setId(51L);
                    return session;
                });
    }

    @Test
    void teacherCanProposeToTeachTheirSkill() {
        authenticateAs(teacher);

        ClassSessionResponseDTO response = sessionService.proposeSession(
                17L, createDTO(), authentication);

        assertEquals(teacher.getId(), response.getProposerId());
        assertEquals("JavaScript", response.getTeachingSkillName());
        assertEquals(SessionStatus.PROPOSED, response.getStatus());
        verify(notificationService).createNotification(
                learner,
                NotificationType.SESSION_PROPOSED,
                "Shriya proposed a class for JavaScript");
    }

    @Test
    void learnerCanProposeToTeachTheirReciprocalSkill() {
        authenticateAs(learner);

        ClassSessionResponseDTO response = sessionService.proposeSession(
                17L, createDTO(), authentication);

        assertEquals(learner.getId(), response.getProposerId());
        assertEquals("Java", response.getTeachingSkillName());
        assertEquals(SessionStatus.PROPOSED, response.getStatus());
        verify(notificationService).createNotification(
                teacher,
                NotificationType.SESSION_PROPOSED,
                "Payal proposed a class for Java");
    }

    @Test
    void bothParticipantsCanProposeMultipleSessionsForTheirSkills() {
        authenticateAs(teacher);
        sessionService.proposeSession(17L, createDTO(), authentication);
        sessionService.proposeSession(17L, createDTO(), authentication);

        authenticateAs(learner);
        sessionService.proposeSession(17L, createDTO(), authentication);
        sessionService.proposeSession(17L, createDTO(), authentication);

        verify(sessionRepository, times(4)).save(any(ClassSession.class));
        verify(notificationService, times(4)).createNotification(
                any(User.class),
                eq(NotificationType.SESSION_PROPOSED),
                anyString());
    }

    @Test
    void onlyTheOtherParticipantCanConfirmAProposal() {
        authenticateAs(learner);
        ClassSession proposedSession = session(teacher);
        when(sessionRepository.findById(51L))
                .thenReturn(Optional.of(proposedSession));
        ClassSessionDecisionDTO decision = new ClassSessionDecisionDTO();
        decision.setStatus(SessionStatus.CONFIRMED);

        ClassSessionResponseDTO response = sessionService.respondToSession(
                51L, decision, authentication);

        assertEquals(SessionStatus.CONFIRMED, response.getStatus());
        verify(notificationService).createNotification(
                teacher,
                NotificationType.SESSION_CONFIRMED,
                "Payal confirmed the class to teach JavaScript");
    }

    @Test
    void proposerCannotConfirmTheirOwnProposal() {
        authenticateAs(teacher);
        when(sessionRepository.findById(51L))
                .thenReturn(Optional.of(session(teacher)));
        ClassSessionDecisionDTO decision = new ClassSessionDecisionDTO();
        decision.setStatus(SessionStatus.CONFIRMED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.respondToSession(
                        51L, decision, authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }

    @Test
    void reciprocalProposerMustHaveAnOfferedTeachingSkill() {
        authenticateAs(learner);
        acceptedRequest.setLearnerSkill(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.proposeSession(
                        17L, createDTO(), authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }

    @Test
    void nonParticipantCannotProposeASession() {
        authenticateAs(outsider);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.proposeSession(
                        17L, createDTO(), authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }

    @Test
    void sessionCannotBeCancelledAtOrAfterItsStartTime() {
        authenticateAs(teacher);
        ClassSession startedSession = session(teacher);
        startedSession.setScheduledAt(LocalDateTime.now().minusSeconds(1));
        startedSession.setStatus(SessionStatus.CONFIRMED);
        when(sessionRepository.findById(51L))
                .thenReturn(Optional.of(startedSession));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.cancelSession(51L, authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sessionRepository, never()).save(any(ClassSession.class));
    }

    private void authenticateAs(User user) {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    private ClassSessionCreateDTO createDTO() {
        ClassSessionCreateDTO dto = new ClassSessionCreateDTO();
        dto.setScheduledAt(LocalDateTime.now().plusDays(2));
        dto.setDurationMinutes(60);
        return dto;
    }

    private ClassSession session(User proposer) {
        ClassSession session = new ClassSession();
        session.setId(51L);
        session.setSkillExchangeRequest(acceptedRequest);
        session.setProposer(proposer);
        session.setScheduledAt(LocalDateTime.now().plusDays(2));
        session.setDurationMinutes(60);
        session.setStatus(SessionStatus.PROPOSED);
        return session;
    }

    private User user(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(Role.USER);
        return user;
    }

    private Skill skill(Long id, User owner, String name, SkillType type) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setUser(owner);
        skill.setName(name);
        skill.setType(type);
        return skill;
    }
}
