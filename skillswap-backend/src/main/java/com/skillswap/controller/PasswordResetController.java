package com.skillswap.controller;

import com.skillswap.dto.ForgotPasswordRequest;
import com.skillswap.dto.VerifyOtpRequest;
import com.skillswap.dto.OtpVerificationResponse;
import com.skillswap.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skillswap.repository.UserRepository;
import com.skillswap.dto.ResetPasswordRequest;

@RestController
@RequestMapping("/api/users")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    public PasswordResetController(
            PasswordResetService passwordResetService,  UserRepository userRepository) {
        this.passwordResetService = passwordResetService;
        this.userRepository = userRepository;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.generateAndSendOtp(request.email());

        return ResponseEntity.ok(
                "If the account exists, a password reset OTP will be sent."
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
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        boolean reset = passwordResetService.resetPassword(
                request.resetToken(),
                request.newPassword()
        );

        if (!reset) {
            return ResponseEntity.badRequest()
                    .body("Invalid or expired reset token");
        }

        return ResponseEntity.ok("Password reset successfully");
    }

}
