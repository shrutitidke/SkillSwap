package com.skillswap.repository;

import com.skillswap.entity.SessionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionReviewRepository
        extends JpaRepository<SessionReview, Long> {

    boolean existsBySessionIdAndReviewerId(Long sessionId, Long reviewerId);

    @Query("""
            SELECT CASE WHEN COUNT(review) > 0 THEN true ELSE false END
            FROM SessionReview review
            WHERE review.session.skillExchangeRequest.id = :requestId
              AND review.reviewer.id = :reviewerId
            """)
    boolean existsByRequestAndReviewer(
            @Param("requestId") Long requestId,
            @Param("reviewerId") Long reviewerId);

    Optional<SessionReview> findBySessionIdAndReviewerId(
            Long sessionId,
            Long reviewerId);

    List<SessionReview> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<SessionReview> findByRevieweeIdOrderByCreatedAtDescIdDesc(
            Long revieweeId);

    void deleteByReviewerIdOrRevieweeId(Long reviewerId, Long revieweeId);

    void deleteBySessionSkillExchangeRequestIdIn(List<Long> requestIds);
}
