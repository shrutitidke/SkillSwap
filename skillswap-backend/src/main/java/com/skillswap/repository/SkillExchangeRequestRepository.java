package com.skillswap.repository;

import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.enums.RequestStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillExchangeRequestRepository
        extends JpaRepository<SkillExchangeRequest, Long> {

    List<SkillExchangeRequest> findByLearnerIdOrderByCreatedAtDesc(
            Long learnerId);

    List<SkillExchangeRequest> findByTeacherIdOrderByCreatedAtDesc(
            Long teacherId);

    List<SkillExchangeRequest> findByLearnerIdOrTeacherId(
            Long learnerId,
            Long teacherId);

    @Query("""
            select case when count(request) > 0 then true else false end
            from SkillExchangeRequest request
            where (
                (request.learner.id = :learnerId
                    and request.teacher.id = :teacherId
                    and request.skill.id = :teacherSkillId
                    and request.learnerSkill.id = :learnerSkillId)
                or
                (request.learner.id = :teacherId
                    and request.teacher.id = :learnerId
                    and request.skill.id = :learnerSkillId
                    and request.learnerSkill.id = :teacherSkillId)
            )
            and (
                request.status = :pendingStatus
                or (
                    request.status = :acceptedStatus
                    and (
                        request.skill.updatedAt <= request.updatedAt
                        or request.learnerSkill.updatedAt <= request.updatedAt
                    )
                )
            )
            """)
    boolean existsActiveExchangeRequest(
            @Param("learnerId") Long learnerId,
            @Param("teacherId") Long teacherId,
            @Param("teacherSkillId") Long teacherSkillId,
            @Param("learnerSkillId") Long learnerSkillId,
            @Param("pendingStatus") RequestStatus pendingStatus,
            @Param("acceptedStatus") RequestStatus acceptedStatus);
}
