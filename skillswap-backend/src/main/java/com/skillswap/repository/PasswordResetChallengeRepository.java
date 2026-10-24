package com.skillswap.repository;

import com.skillswap.entity.PasswordResetChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetChallengeRepository
        extends JpaRepository<PasswordResetChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from PasswordResetChallenge challenge where challenge.user.id = :userId")
    Optional<PasswordResetChallenge> findByUserIdForUpdate(
            @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetChallenge> findByResetAuthorizationSelector(
            String selector);

    void deleteByUserId(Long userId);
}
