package com.skillswap.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_challenges")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 60)
    private String otpHash;

    private LocalDateTime otpExpiresAt;

    @Column(nullable = false)
    private int verificationAttempts;

    @Column(nullable = false)
    private boolean otpUsed = true;

    private LocalDateTime lastRequestedAt;
    private LocalDateTime requestWindowStartedAt;

    @Column(nullable = false)
    private int requestsInWindow;

    @Column(length = 22, unique = true)
    private String resetAuthorizationSelector;

    @Column(length = 60)
    private String resetAuthorizationHash;

    private LocalDateTime resetAuthorizationExpiresAt;

    public PasswordResetChallenge(User user) {
        this.user = user;
    }

    public void invalidateOtp() {
        otpHash = null;
        otpExpiresAt = null;
        otpUsed = true;
    }

    public void invalidateResetAuthorization() {
        resetAuthorizationSelector = null;
        resetAuthorizationHash = null;
        resetAuthorizationExpiresAt = null;
    }
}
