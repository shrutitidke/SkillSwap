package com.skillswap.repository;

import com.skillswap.entity.SkillExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillExchangeRequestRepository
        extends JpaRepository<SkillExchangeRequest, Long> {

    List<SkillExchangeRequest> findByLearnerIdOrderByCreatedAtDesc(
            Long learnerId);

    List<SkillExchangeRequest> findByTeacherIdOrderByCreatedAtDesc(
            Long teacherId);

    boolean existsByLearnerIdAndSkillId(
            Long learnerId, Long skillId);
}
