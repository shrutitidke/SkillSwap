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
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.SkillRepository;
import com.skillswap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillExchangeRequestServiceTest {

    @Mock
    private SkillExchangeRequestRepository requestRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    private SkillExchangeRequestService requestService;
    private User learner;
    private User teacher;
    private Skill learnerTeachesJavaScript;
    private Skill learnerLearnsJava;
    private Skill teacherTeachesJava;
    private Skill teacherLearnsJavaScript;

    @BeforeEach
    void setUp() {
        requestService = new SkillExchangeRequestService(
                requestRepository,
                skillRepository,
                userRepository,
                notificationService);

        learner = user(1L, "Learner", "learner@example.test");
        teacher = user(2L, "Teacher", "teacher@example.test");
        learnerTeachesJavaScript = skill(
                11L, learner, "JavaScript", SkillType.TEACH);
        learnerLearnsJava = skill(
                12L, learner, "Java", SkillType.LEARN);
        teacherTeachesJava = skill(
                21L, teacher, "Java", SkillType.TEACH);
        teacherLearnsJavaScript = skill(
                22L, teacher, "JavaScript", SkillType.LEARN);

        lenient().when(authentication.getName()).thenReturn(learner.getEmail());
        lenient().when(userRepository.findByEmail(learner.getEmail()))
                .thenReturn(Optional.of(learner));
    }

    @Test
    void sendRequestPersistsBothSkillsAndNotifiesTeacher() {
        when(skillRepository.findById(teacherTeachesJava.getId()))
                .thenReturn(Optional.of(teacherTeachesJava));
        when(skillRepository.findByUserId(learner.getId()))
                .thenReturn(List.of(
                        learnerTeachesJavaScript,
                        learnerLearnsJava));
        when(skillRepository.findByUserId(teacher.getId()))
                .thenReturn(List.of(
                        teacherTeachesJava,
                        teacherLearnsJavaScript));
        when(requestRepository
                .existsByLearnerIdAndSkillIdAndLearnerSkillIdAndStatus(
                        learner.getId(),
                        teacherTeachesJava.getId(),
                        learnerTeachesJavaScript.getId(),
                        RequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.save(any(SkillExchangeRequest.class)))
                .thenAnswer(invocation -> {
                    SkillExchangeRequest saved = invocation.getArgument(0);
                    saved.setId(101L);
                    return saved;
                });

        SkillExchangeRequestCreateDTO request = new SkillExchangeRequestCreateDTO();
        request.setSkillId(teacherTeachesJava.getId());
        request.setLearnerSkillId(learnerTeachesJavaScript.getId());
        request.setMessage("Happy to exchange lessons.");

        SkillExchangeRequestResponseDTO response =
                requestService.sendRequest(request, authentication);

        assertEquals(101L, response.getId());
        assertEquals("Java", response.getSkillName());
        assertEquals("JavaScript", response.getLearnerSkillName());
        assertEquals(RequestStatus.PENDING, response.getStatus());
        verify(requestRepository).save(any(SkillExchangeRequest.class));
        verify(notificationService).createNotification(
                teacher,
                NotificationType.SKILL_REQUEST_RECEIVED,
                "Learner has requested to learn Java");
    }

    @Test
    void sendRequestRejectsANonReciprocalExchange() {
        Skill teacherLearnsPython =
                skill(23L, teacher, "Python", SkillType.LEARN);
        when(skillRepository.findById(teacherTeachesJava.getId()))
                .thenReturn(Optional.of(teacherTeachesJava));
        when(skillRepository.findByUserId(learner.getId()))
                .thenReturn(List.of(
                        learnerTeachesJavaScript,
                        learnerLearnsJava));
        when(skillRepository.findByUserId(teacher.getId()))
                .thenReturn(List.of(
                        teacherTeachesJava,
                        teacherLearnsPython));

        SkillExchangeRequestCreateDTO request = new SkillExchangeRequestCreateDTO();
        request.setSkillId(teacherTeachesJava.getId());
        request.setLearnerSkillId(learnerTeachesJavaScript.getId());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestService.sendRequest(request, authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(requestRepository, never()).save(any());
        verify(notificationService, never())
                .createNotification(any(), any(), any());
    }

    @Test
    void sendRequestRejectsAnExistingPendingDuplicate() {
        when(skillRepository.findById(teacherTeachesJava.getId()))
                .thenReturn(Optional.of(teacherTeachesJava));
        when(skillRepository.findByUserId(learner.getId()))
                .thenReturn(List.of(
                        learnerTeachesJavaScript,
                        learnerLearnsJava));
        when(skillRepository.findByUserId(teacher.getId()))
                .thenReturn(List.of(
                        teacherTeachesJava,
                        teacherLearnsJavaScript));
        when(requestRepository
                .existsByLearnerIdAndSkillIdAndLearnerSkillIdAndStatus(
                        learner.getId(),
                        teacherTeachesJava.getId(),
                        learnerTeachesJavaScript.getId(),
                        RequestStatus.PENDING))
                .thenReturn(true);

        SkillExchangeRequestCreateDTO request = new SkillExchangeRequestCreateDTO();
        request.setSkillId(teacherTeachesJava.getId());
        request.setLearnerSkillId(learnerTeachesJavaScript.getId());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestService.sendRequest(request, authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void onlyTheReceivingTeacherCanRespond() {
        SkillExchangeRequest existing = new SkillExchangeRequest();
        existing.setId(101L);
        existing.setLearner(learner);
        existing.setTeacher(teacher);
        existing.setSkill(teacherTeachesJava);
        existing.setStatus(RequestStatus.PENDING);
        when(requestRepository.findById(101L))
                .thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestService.respondToRequest(
                        101L,
                        RequestStatus.ACCEPTED,
                        authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(requestRepository, never()).save(any());
        verify(notificationService, never())
                .createNotification(any(), any(), any());
    }

    @Test
    void receivingTeacherCanAcceptPendingRequestAndNotifySender() {
        when(authentication.getName()).thenReturn(teacher.getEmail());
        when(userRepository.findByEmail(teacher.getEmail()))
                .thenReturn(Optional.of(teacher));

        SkillExchangeRequest existing = new SkillExchangeRequest();
        existing.setId(101L);
        existing.setLearner(learner);
        existing.setTeacher(teacher);
        existing.setSkill(teacherTeachesJava);
        existing.setLearnerSkill(learnerTeachesJavaScript);
        existing.setStatus(RequestStatus.PENDING);
        when(requestRepository.findById(101L))
                .thenReturn(Optional.of(existing));
        when(requestRepository.save(existing)).thenReturn(existing);

        SkillExchangeRequestResponseDTO response =
                requestService.respondToRequest(
                        101L,
                        RequestStatus.ACCEPTED,
                        authentication);

        assertEquals(RequestStatus.ACCEPTED, response.getStatus());
        verify(requestRepository).save(existing);
        verify(notificationService).createNotification(
                learner,
                NotificationType.SKILL_REQUEST_ACCEPTED,
                "Teacher accepted your request to learn Java");
    }

    @Test
    void cannotRespondWithPendingAsADecision() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> requestService.respondToRequest(
                        101L,
                        RequestStatus.PENDING,
                        authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(requestRepository, never()).findById(101L);
    }

    @Test
    void sentRequestsAreScopedToTheAuthenticatedLearner() {
        when(requestRepository.findByLearnerIdOrderByCreatedAtDesc(
                learner.getId())).thenReturn(List.of());

        List<SkillExchangeRequestResponseDTO> result =
                requestService.getSentRequests(authentication);

        assertEquals(0, result.size());
        verify(requestRepository)
                .findByLearnerIdOrderByCreatedAtDesc(learner.getId());
    }

    @Test
    void receivedRequestsAreScopedToTheAuthenticatedTeacher() {
        when(requestRepository.findByTeacherIdOrderByCreatedAtDesc(
                learner.getId())).thenReturn(List.of());

        List<SkillExchangeRequestResponseDTO> result =
                requestService.getReceivedRequests(authentication);

        assertEquals(0, result.size());
        verify(requestRepository)
                .findByTeacherIdOrderByCreatedAtDesc(learner.getId());
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
