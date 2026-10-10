package com.skillswap.dto;

public record OtpVerificationResponse(
        String message,
        String resetToken
) {}
