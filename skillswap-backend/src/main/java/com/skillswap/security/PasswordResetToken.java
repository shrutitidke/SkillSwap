package com.skillswap.security;

import java.time.LocalDateTime;

public class PasswordResetToken {

    private final String otp;
    private final LocalDateTime expiryTime;
    private int attempts;
    private String resetToken;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public PasswordResetToken(String otp, LocalDateTime expiryTime) {
        this.otp = otp;
        this.expiryTime = expiryTime;
        this.attempts = 0;
    }

    public String getOtp() {
        return otp;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public boolean hasExceededAttempts() {
        return attempts >= 5;
    }
}
