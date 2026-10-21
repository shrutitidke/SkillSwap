package com.skillswap.service;

import com.skillswap.dto.PublicReviewDTO;
import com.skillswap.dto.PublicUserProfileDTO;
import com.skillswap.dto.SessionReviewCreateDTO;
import com.skillswap.dto.SessionReviewResponseDTO;
import com.skillswap.entity.ClassSession;
import com.skillswap.entity.SessionReview;
import com.skillswap.entity.User;
import com.skillswap.enums.Role;
import com.skillswap.enums.SessionStatus;
import com.skillswap.repository.ClassSessionRepository;
import com.skillswap.repository.SessionReviewRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionReviewService {

    private final SessionReviewRepository reviewRepository;
    private final ClassSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionReviewService(
            SessionReviewRepository reviewRepository,
            ClassSessionRepository sessionRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SessionReviewResponseDTO createReview(
            Long sessionId,
            SessionReviewCreateDTO request,
            Authentication authentication) {
        User reviewer = requireUser(authentication);
        ClassSession session = requireParticipantSession(sessionId, reviewer);
        LocalDateTime scheduledEnd = session.getScheduledAt()
                .plusMinutes(session.getDurationMinutes());

        if (session.getStatus() != SessionStatus.CONFIRMED
                || scheduledEnd.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reviews are available after a confirmed session ends");
        }

        User reviewee = otherParticipant(session, reviewer);
        if (reviewRepository.existsBySessionIdAndReviewerId(
                sessionId, reviewer.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this session");
        }

        SessionReview review = new SessionReview();
        review.setSession(session);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));

        try {
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already reviewed this session",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<SessionReviewResponseDTO> getSessionReviews(
            Long sessionId,
            Authentication authentication) {
        User currentUser = requireUser(authentication);
        requireParticipantSession(sessionId, currentUser);
        return reviewRepository
                .findBySessionIdOrderByCreatedAtAscIdAsc(sessionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicUserProfileDTO getPublicProfile(Long userId) {
        User reviewee = userRepository.findById(userId)
                .filter(user -> user.getRole() == Role.USER)
                .orElseThrow(this::profileNotFound);
        List<SessionReview> reviews =
                reviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(
                        userId);
        double averageRating = reviews.isEmpty()
                ? 0
                : Math.round(reviews.stream()
                        .mapToInt(SessionReview::getRating)
                        .average()
                        .orElse(0) * 10.0) / 10.0;

        List<PublicReviewDTO> publicReviews = reviews.stream()
                .map(review -> new PublicReviewDTO(
                        review.getId(),
                        review.getReviewer().getName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()))
                .toList();

        return new PublicUserProfileDTO(
                reviewee.getId(),
                reviewee.getName(),
                averageRating,
                reviews.size(),
                publicReviews);
    }

    private User requireUser(Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(this::reviewNotFound);
        if (currentUser.getRole() != Role.USER) {
            throw reviewNotFound();
        }
        return currentUser;
    }

    private ClassSession requireParticipantSession(
            Long sessionId,
            User currentUser) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(this::reviewNotFound);
        boolean isParticipant =
                session.getSkillExchangeRequest().getLearner().getId()
                        .equals(currentUser.getId())
                        || session.getSkillExchangeRequest().getTeacher()
                                .getId().equals(currentUser.getId());
        if (!isParticipant) {
            throw reviewNotFound();
        }
        return session;
    }

    private User otherParticipant(ClassSession session, User reviewer) {
        var request = session.getSkillExchangeRequest();
        if (request.getLearner().getId().equals(reviewer.getId())) {
            return request.getTeacher();
        }
        return request.getLearner();
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }

    private SessionReviewResponseDTO toResponse(SessionReview review) {
        return new SessionReviewResponseDTO(
                review.getId(),
                review.getSession().getId(),
                review.getReviewer().getId(),
                review.getReviewer().getName(),
                review.getReviewee().getId(),
                review.getReviewee().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    private ResponseStatusException reviewNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Session review not found");
    }

    private ResponseStatusException profileNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Public profile not found");
    }
}
