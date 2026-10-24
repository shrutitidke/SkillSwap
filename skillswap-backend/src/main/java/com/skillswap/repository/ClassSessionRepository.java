package com.skillswap.repository;

import com.skillswap.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassSessionRepository
        extends JpaRepository<ClassSession, Long> {

    List<ClassSession> findBySkillExchangeRequestIdOrderByScheduledAtAsc(
            Long requestId);

    List<ClassSession>
    findBySkillExchangeRequestTeacherIdOrSkillExchangeRequestLearnerIdOrderByScheduledAtAsc(
            Long teacherId, Long learnerId);

    void deleteBySkillExchangeRequestIdIn(List<Long> requestIds);
}
