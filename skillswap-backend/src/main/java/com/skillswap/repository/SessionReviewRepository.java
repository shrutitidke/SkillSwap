package com.skillswap.repository;

import com.skillswap.entity.SessionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionReviewRepository
        extends JpaRepository<SessionReview, Long> {

    boolean existsBySessionIdAndReviewerId(Long sessionId, Long reviewerId);

    Optional<SessionReview> findBySessionIdAndReviewerId(
            Long sessionId,
            Long reviewerId);

    List<SessionReview> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<SessionReview> findByRevieweeIdOrderByCreatedAtDescIdDesc(
            Long revieweeId);
}
