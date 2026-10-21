package com.skillswap.controller;

import com.skillswap.dto.ForgotPasswordRequest;
import com.skillswap.dto.MessageResponse;
import com.skillswap.dto.OtpVerificationResponse;
import com.skillswap.dto.ResetPasswordRequest;
import com.skillswap.dto.VerifyOtpRequest;
import com.skillswap.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.generateAndSendOtp(request.email());

        return ResponseEntity.ok(
                new MessageResponse(
                        "If the account exists, a password reset OTP will be sent.")
        );
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<OtpVerificationResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        OtpVerificationResponse response =
                passwordResetService.verifyOtp(
                        request.email(),
                        request.otp()
                );

        if (response.resetToken() == null) {
            HttpStatus status = response.message().startsWith("Too many attempts")
                    ? HttpStatus.TOO_MANY_REQUESTS
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        boolean reset = passwordResetService.resetPassword(
                request.resetToken(),
                request.newPassword()
        );

        if (!reset) {
            return ResponseEntity.badRequest().body(
                    new MessageResponse("Invalid or expired reset token"));
        }

        return ResponseEntity.ok(
                new MessageResponse("Password reset successfully"));
    }

}
