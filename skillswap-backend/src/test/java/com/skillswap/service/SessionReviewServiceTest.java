package com.skillswap.service;

import com.skillswap.dto.SessionReviewCreateDTO;
import com.skillswap.dto.SessionReviewResponseDTO;
import com.skillswap.entity.ClassSession;
import com.skillswap.entity.SessionReview;
import com.skillswap.entity.Skill;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.Role;
import com.skillswap.enums.SessionStatus;
import com.skillswap.repository.ClassSessionRepository;
import com.skillswap.repository.SessionReviewRepository;
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
class SessionReviewServiceTest {

    @Mock
    private SessionReviewRepository reviewRepository;

    @Mock
    private ClassSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private SessionReviewService reviewService;
    private User learner;
    private User teacher;
    private User outsider;
    private SkillExchangeRequest exchange;
    private ClassSession completedSession;

    @BeforeEach
    void setUp() {
        reviewService = new SessionReviewService(
                reviewRepository, sessionRepository, userRepository);
        learner = user(1L, "Learner", "learner@example.test", Role.USER);
        teacher = user(2L, "Teacher", "teacher@example.test", Role.USER);
        outsider = user(3L, "Outsider", "outsider@example.test", Role.USER);

        exchange = new SkillExchangeRequest();
        exchange.setId(10L);
        exchange.setLearner(learner);
        exchange.setTeacher(teacher);
        exchange.setStatus(RequestStatus.ACCEPTED);
        Skill teachingSkill = new Skill();
        teachingSkill.setName("Java");
        exchange.setSkill(teachingSkill);

        completedSession = new ClassSession();
        completedSession.setId(20L);
        completedSession.setSkillExchangeRequest(exchange);
        completedSession.setScheduledAt(LocalDateTime.now().minusHours(2));
        completedSession.setDurationMinutes(60);
        completedSession.setStatus(SessionStatus.CONFIRMED);

        lenient().when(authentication.getName()).thenReturn(learner.getEmail());
        lenient().when(userRepository.findByEmail(learner.getEmail()))
                .thenReturn(Optional.of(learner));
        lenient().when(sessionRepository.findById(20L))
                .thenReturn(Optional.of(completedSession));
    }

    @Test
    void learnerCanReviewTeacherAfterCourseAndLatestSessionComplete() {
        exchange.setUpdatedAt(LocalDateTime.now().minusDays(11));
        when(sessionRepository
                .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                        1L, 1L))
                .thenReturn(List.of(completedSession));
        when(reviewRepository.existsByRequestAndReviewer(10L, 1L))
                .thenReturn(false);
        when(reviewRepository.existsBySessionIdAndReviewerId(20L, 1L))
                .thenReturn(false);
        when(reviewRepository.saveAndFlush(any(SessionReview.class)))
                .thenAnswer(invocation -> {
                    SessionReview review = invocation.getArgument(0);
                    review.setId(30L);
                    review.setCreatedAt(LocalDateTime.now());
                    return review;
                });

        SessionReviewResponseDTO response = reviewService.createReview(
                20L,
                new SessionReviewCreateDTO(5, "  Great lesson!  "),
                authentication);

        assertEquals(1L, response.reviewerId());
        assertEquals(2L, response.revieweeId());
        assertEquals(5, response.rating());
        assertEquals("Great lesson!", response.comment());
    }

    @Test
    void bothExchangeParticipantsCanReviewEachOther() {
        when(authentication.getName()).thenReturn(teacher.getEmail());
        when(userRepository.findByEmail(teacher.getEmail()))
                .thenReturn(Optional.of(teacher));
        when(reviewRepository.existsBySessionIdAndReviewerId(20L, 2L))
                .thenReturn(false);
        when(reviewRepository.saveAndFlush(any(SessionReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionReviewResponseDTO response = reviewService.createReview(
                20L,
                new SessionReviewCreateDTO(3, null),
                authentication);

        assertEquals(2L, response.reviewerId());
        assertEquals(1L, response.revieweeId());
    }

    @Test
    void cannotReviewBeforeScheduledEnd() {
        completedSession.setScheduledAt(LocalDateTime.now().plusMinutes(1));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        20L,
                        new SessionReviewCreateDTO(5, null),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void learnerCannotRateTeacherBeforeTenDayCourseIsComplete() {
        exchange.setUpdatedAt(LocalDateTime.now().minusDays(9));
        when(sessionRepository
                .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                        1L, 1L))
                .thenReturn(List.of(completedSession));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        20L,
                        new SessionReviewCreateDTO(5, null),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void cannotReviewSessionThatWasNotConfirmed() {
        completedSession.setStatus(SessionStatus.CANCELLED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        20L,
                        new SessionReviewCreateDTO(5, null),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void preventsSecondReviewBySameParticipantForSession() {
        when(reviewRepository.existsBySessionIdAndReviewerId(20L, 1L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        20L,
                        new SessionReviewCreateDTO(4, null),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void requiresCourseRatingAfterTenDaysAndLatestConfirmedSessionEnds() {
        exchange.setUpdatedAt(LocalDateTime.now().minusDays(11));
        ClassSession latestSession = new ClassSession();
        latestSession.setId(21L);
        latestSession.setSkillExchangeRequest(exchange);
        latestSession.setScheduledAt(LocalDateTime.now().minusHours(1));
        latestSession.setDurationMinutes(30);
        latestSession.setStatus(SessionStatus.CONFIRMED);
        when(sessionRepository
                .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                        1L, 1L))
                .thenReturn(List.of(completedSession, latestSession));
        when(reviewRepository.existsByRequestAndReviewer(10L, 1L))
                .thenReturn(false);

        var requiredReviews =
                reviewService.getRequiredCourseReviews(authentication);

        assertEquals(1, requiredReviews.size());
        assertEquals(21L, requiredReviews.get(0).sessionId());
        assertEquals(2L, requiredReviews.get(0).teacherId());
    }

    @Test
    void doesNotRequireCourseRatingBeforeTenDays() {
        exchange.setUpdatedAt(LocalDateTime.now().minusDays(9));
        when(sessionRepository
                .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                        1L, 1L))
                .thenReturn(List.of(completedSession));

        var requiredReviews =
                reviewService.getRequiredCourseReviews(authentication);

        assertEquals(0, requiredReviews.size());
    }

    @Test
    void requiresRatingForLatestCompletedSessionWhenCourseReviewIsDue() {
        exchange.setUpdatedAt(LocalDateTime.now().minusDays(11));
        ClassSession latestSession = new ClassSession();
        latestSession.setId(21L);
        latestSession.setSkillExchangeRequest(exchange);
        latestSession.setScheduledAt(LocalDateTime.now().minusHours(1));
        latestSession.setDurationMinutes(30);
        latestSession.setStatus(SessionStatus.CONFIRMED);
        when(sessionRepository
                .findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
                        1L, 1L))
                .thenReturn(List.of(completedSession, latestSession));
        when(reviewRepository.existsByRequestAndReviewer(10L, 1L))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        20L,
                        new SessionReviewCreateDTO(5, "Good course"),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNonParticipantSessionAccess() {
        when(authentication.getName()).thenReturn(outsider.getEmail());
        when(userRepository.findByEmail(outsider.getEmail()))
                .thenReturn(Optional.of(outsider));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.getSessionReviews(20L, authentication));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(reviewRepository, never())
                .findBySessionIdOrderByCreatedAtAscIdAsc(20L);
    }

    @Test
    void publicProfileContainsDisplayNameAndReviewsWithoutPrivateFields() {
        SessionReview review = new SessionReview();
        review.setId(30L);
        review.setReviewer(teacher);
        review.setReviewee(learner);
        review.setRating(4);
        review.setComment("Helpful exchange");
        review.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(learner));
        when(reviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(List.of(review));

        var profile = reviewService.getPublicProfile(1L);

        assertEquals(1L, profile.id());
        assertEquals("Learner", profile.name());
        assertEquals(4.0, profile.averageRating());
        assertEquals(1L, profile.reviewCount());
        assertEquals("Teacher", profile.reviews().get(0).reviewerName());
    }

    private User user(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
