package com.skillswap.service;

import com.skillswap.security.PasswordResetToken;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import com.skillswap.dto.OtpVerificationResponse;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import com.skillswap.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.skillswap.entity.User;

@Service
public class PasswordResetService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, PasswordResetToken> resetTokens =
            new ConcurrentHashMap<>();



    private final Map<String, String> verifiedResetTokens =
            new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> resetTokenExpiry =
            new ConcurrentHashMap<>();

    public PasswordResetService(EmailService emailService, UserRepository userRepository,  PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void generateAndSendOtp(String email) {

        // Check whether the email belongs to a registered user
        if (!userRepository.existsByEmail(email)) {
            return;
        }

        // Generate a random 6-digit OTP
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // Set OTP expiry time to 5 minutes
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        // Create the password reset token
        PasswordResetToken resetToken =
                new PasswordResetToken(otp, expiryTime);

        // Store the OTP temporarily
        resetTokens.put(email, resetToken);

        // Send the OTP to the registered email
        emailService.sendOtpEmail(email, otp);
    }

    public OtpVerificationResponse verifyOtp(
            String email, String enteredOtp) {

        PasswordResetToken resetToken = resetTokens.get(email);

        if (resetToken == null) {
            return new OtpVerificationResponse(
                    "Invalid or expired OTP", null);
        }

        if (resetToken.isExpired()) {
            resetTokens.remove(email, resetToken);
            return new OtpVerificationResponse(
                    "Invalid or expired OTP", null);
        }

        if (resetToken.hasExceededAttempts()) {
            resetTokens.remove(email, resetToken);
            return new OtpVerificationResponse(
                    "Invalid or expired OTP", null);
        }

        if (!resetToken.getOtp().equals(enteredOtp)) {
            resetToken.incrementAttempts();

            if (resetToken.hasExceededAttempts()) {
                resetTokens.remove(email, resetToken);
            }

            return new OtpVerificationResponse(
                    "Invalid or expired OTP", null);
        }

        // OTP is correct; remove it so it cannot be reused.
        resetTokens.remove(email, resetToken);

        // Generate a separate reset authorization token.
        String newResetToken = UUID.randomUUID().toString();

        verifiedResetTokens.put(newResetToken, email);
        resetTokenExpiry.put(
                newResetToken,
                LocalDateTime.now().plusMinutes(10)
        );

        return new OtpVerificationResponse(
                "OTP verified successfully",
                newResetToken
        );
    }

    public boolean resetPassword(String token, String newPassword) {

        // Find the email associated with the reset token
        String email = verifiedResetTokens.get(token);

        if (email == null) {
            return false;
        }

        // Check whether the reset token has expired
        LocalDateTime expiryTime = resetTokenExpiry.get(token);

        if (expiryTime == null || LocalDateTime.now().isAfter(expiryTime)) {
            verifiedResetTokens.remove(token);
            resetTokenExpiry.remove(token);
            return false;
        }

        // Find the registered user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            verifiedResetTokens.remove(token);
            resetTokenExpiry.remove(token);
            return false;
        }

        // Encode the new password using BCrypt
        user.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated password in MySQL
        userRepository.save(user);

        // Remove the token so it cannot be reused
        verifiedResetTokens.remove(token);
        resetTokenExpiry.remove(token);

        return true;
    }



}
