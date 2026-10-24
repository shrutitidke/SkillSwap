package com.skillswap.repository;

import com.skillswap.entity.Skill;
import com.skillswap.enums.Role;
import com.skillswap.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByUserId(Long userId);

    List<Skill> findByUserIdIn(List<Long> userIds);

    void deleteByUserId(Long userId);

    Optional<Skill> findByIdAndUserId(Long skillId, Long userId);


    @Query("""
        SELECT s FROM Skill s
        JOIN FETCH s.user u
        WHERE (:type IS NULL OR s.type = :type)
        AND u.id <> :userId
        AND u.role <> :role
        ORDER BY s.name ASC
        """)
    List<Skill> findDiscoverableSkills(
            @Param("type") SkillType type,
            @Param("userId") Long userId,
            @Param("role") Role role
    );

}
