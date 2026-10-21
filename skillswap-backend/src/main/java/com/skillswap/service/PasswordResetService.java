package com.skillswap.service;

import com.skillswap.dto.OtpVerificationResponse;
import com.skillswap.entity.PasswordResetChallenge;
import com.skillswap.entity.User;
import com.skillswap.repository.PasswordResetChallengeRepository;
import com.skillswap.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PasswordResetService.class);
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordResetChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            EmailService emailService,
            UserRepository userRepository,
            PasswordResetChallengeRepository challengeRepository,
            PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendOtp(String email) {
        Optional<User> account = userRepository.findByEmailIgnoreCase(
                email.trim());
        if (account.isEmpty()) {
            return;
        }

        User user = account.get();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetChallenge challenge = challengeRepository
                .findByUserIdForUpdate(user.getId())
                .orElseGet(() -> new PasswordResetChallenge(user));

        if (isRequestLimited(challenge, now)) {
            return;
        }

        updateRequestWindow(challenge, now);
        challenge.setRequestsInWindow(challenge.getRequestsInWindow() + 1);
        challenge.setLastRequestedAt(now);

        String otp = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000));

        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setOtpExpiresAt(now.plusMinutes(5));
        challenge.setVerificationAttempts(0);
        challenge.setOtpUsed(false);
        challenge.invalidateResetAuthorization();
        challengeRepository.saveAndFlush(challenge);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (MailException | IllegalStateException exception) {
            challenge.invalidateOtp();
            challengeRepository.save(challenge);
            LOGGER.warn(
                    "Password-reset OTP delivery failed; check mail configuration.");
        }
    }

    @Transactional
    public OtpVerificationResponse verifyOtp(
            String email,
            String enteredOtp) {
        Optional<User> account = userRepository.findByEmailIgnoreCase(
                email.trim());
        if (account.isEmpty()) {
            return invalidOtpResponse();
        }

        PasswordResetChallenge challenge = challengeRepository
                .findByUserIdForUpdate(account.get().getId())
                .orElse(null);
        if (challenge == null || challenge.isOtpUsed()
                || challenge.getOtpHash() == null
                || challenge.getOtpExpiresAt() == null
                || !challenge.getOtpExpiresAt().isAfter(LocalDateTime.now())) {
            return invalidOtpResponse();
        }

        if (challenge.getVerificationAttempts()
                >= MAX_VERIFICATION_ATTEMPTS) {
            challenge.invalidateOtp();
            challengeRepository.save(challenge);
            return new OtpVerificationResponse(
                    "Too many attempts. Request a new OTP.",
                    null);
        }

        if (!passwordEncoder.matches(enteredOtp, challenge.getOtpHash())) {
            int attempts = challenge.getVerificationAttempts() + 1;
            challenge.setVerificationAttempts(attempts);
            if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
                challenge.invalidateOtp();
                challengeRepository.save(challenge);
                return new OtpVerificationResponse(
                        "Too many attempts. Request a new OTP.",
                        null);
            }
            challengeRepository.save(challenge);
            return invalidOtpResponse();
        }

        challenge.invalidateOtp();

        String selector = randomUrlSafeValue(16);
        String verifier = randomUrlSafeValue(32);
        challenge.setResetAuthorizationSelector(selector);
        challenge.setResetAuthorizationHash(
                passwordEncoder.encode(verifier));
        challenge.setResetAuthorizationExpiresAt(
                LocalDateTime.now().plusMinutes(10));
        challengeRepository.save(challenge);

        return new OtpVerificationResponse(
                "OTP verified successfully",
                selector + "." + verifier);
    }

    @Transactional
    public boolean resetPassword(String resetToken, String newPassword) {
        String[] tokenParts = resetToken.split("\\.", -1);
        if (tokenParts.length != 2
                || tokenParts[0].length() != 22
                || tokenParts[1].length() != 43) {
            return false;
        }

        PasswordResetChallenge challenge = challengeRepository
                .findByResetAuthorizationSelector(tokenParts[0])
                .orElse(null);
        if (challenge == null
                || challenge.getResetAuthorizationHash() == null
                || challenge.getResetAuthorizationExpiresAt() == null
                || !challenge.getResetAuthorizationExpiresAt()
                        .isAfter(LocalDateTime.now())
                || !passwordEncoder.matches(
                        tokenParts[1],
                        challenge.getResetAuthorizationHash())) {
            return false;
        }

        User user = challenge.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        challenge.invalidateOtp();
        challenge.invalidateResetAuthorization();
        challengeRepository.save(challenge);
        return true;
    }

    private boolean isRequestLimited(
            PasswordResetChallenge challenge,
            LocalDateTime now) {
        if (challenge.getLastRequestedAt() != null
                && challenge.getLastRequestedAt().plusSeconds(
                        RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            return true;
        }

        return challenge.getRequestWindowStartedAt() != null
                && challenge.getRequestWindowStartedAt().plusHours(1)
                        .isAfter(now)
                && challenge.getRequestsInWindow() >= MAX_REQUESTS_PER_HOUR;
    }

    private void updateRequestWindow(
            PasswordResetChallenge challenge,
            LocalDateTime now) {
        if (challenge.getRequestWindowStartedAt() == null
                || !challenge.getRequestWindowStartedAt()
                        .plusHours(1).isAfter(now)) {
            challenge.setRequestWindowStartedAt(now);
            challenge.setRequestsInWindow(0);
        }
    }

    private String randomUrlSafeValue(int byteLength) {
        byte[] value = new byte[byteLength];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private OtpVerificationResponse invalidOtpResponse() {
        return new OtpVerificationResponse(
                "Invalid or expired OTP",
                null);
    }
}
