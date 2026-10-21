package com.skillswap.service;

import com.skillswap.dto.OtpVerificationResponse;
import com.skillswap.entity.PasswordResetChallenge;
import com.skillswap.entity.User;
import com.skillswap.repository.PasswordResetChallengeRepository;
import com.skillswap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "member@example.com";

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetChallengeRepository challengeRepository;

    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(4);

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                emailService,
                userRepository,
                challengeRepository,
                passwordEncoder);
    }

    @Test
    void unknownEmailDoesNotCreateChallengeOrSendEmail() {
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.empty());

        passwordResetService.generateAndSendOtp(EMAIL);

        verifyNoInteractions(challengeRepository, emailService);
    }

    @Test
    void otpAndResetAuthorizationAreHashedAndSingleUse() {
        User user = createUser();
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.empty());
        when(challengeRepository.saveAndFlush(any(PasswordResetChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.generateAndSendOtp(EMAIL);

        ArgumentCaptor<PasswordResetChallenge> challengeCaptor =
                ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challengeRepository).saveAndFlush(challengeCaptor.capture());
        PasswordResetChallenge challenge = challengeCaptor.getValue();

        ArgumentCaptor<String> otpCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(EMAIL), otpCaptor.capture());
        String otp = otpCaptor.getValue();
        assertTrue(otp.matches("\\d{6}"));
        assertNotEquals(otp, challenge.getOtpHash());
        assertTrue(passwordEncoder.matches(otp, challenge.getOtpHash()));

        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.of(challenge));
        OtpVerificationResponse verified =
                passwordResetService.verifyOtp(EMAIL, otp);

        assertNotNull(verified.resetToken());
        String[] tokenParts = verified.resetToken().split("\\.");
        assertEquals(2, tokenParts.length);
        assertNotEquals(tokenParts[1], challenge.getResetAuthorizationHash());
        assertTrue(passwordEncoder.matches(
                tokenParts[1],
                challenge.getResetAuthorizationHash()));

        when(challengeRepository.findByResetAuthorizationSelector(tokenParts[0]))
                .thenReturn(Optional.of(challenge));
        when(userRepository.save(user)).thenReturn(user);
        challenge.setResetAuthorizationExpiresAt(
                LocalDateTime.now().minusSeconds(1));
        assertFalse(passwordResetService.resetPassword(
                verified.resetToken(),
                "NewPassword1!"));
        challenge.setResetAuthorizationExpiresAt(
                LocalDateTime.now().plusMinutes(10));
        assertTrue(passwordResetService.resetPassword(
                verified.resetToken(),
                "NewPassword1!"));
        assertFalse(passwordEncoder.matches(
                "OldPassword1!",
                user.getPassword()));
        assertEquals(1, user.getTokenVersion());
        assertTrue(passwordEncoder.matches(
                "NewPassword1!",
                user.getPassword()));
        assertFalse(passwordResetService.resetPassword(
                verified.resetToken(),
                "AnotherPassword1!"));
    }

    @Test
    void incorrectOtpIsLimitedAndExpiredOtpIsRejected() {
        User user = createUser();
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.empty());
        when(challengeRepository.saveAndFlush(any(PasswordResetChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.generateAndSendOtp(EMAIL);
        ArgumentCaptor<PasswordResetChallenge> challengeCaptor =
                ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challengeRepository).saveAndFlush(challengeCaptor.capture());
        PasswordResetChallenge challenge = challengeCaptor.getValue();
        ArgumentCaptor<String> otpCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(EMAIL), otpCaptor.capture());
        String issuedOtp = otpCaptor.getValue();
        String incorrectOtp = "000000".equals(issuedOtp)
                ? "000001"
                : "000000";

        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.of(challenge));

        OtpVerificationResponse result = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            result = passwordResetService.verifyOtp(EMAIL, incorrectOtp);
        }

        assertNotNull(result);
        assertNull(result.resetToken());
        assertTrue(result.message().contains("Too many attempts"));
        assertTrue(challenge.isOtpUsed());

        challenge.setOtpUsed(false);
        challenge.setOtpHash(passwordEncoder.encode("123456"));
        challenge.setOtpExpiresAt(LocalDateTime.now().minusSeconds(1));
        OtpVerificationResponse expired =
                passwordResetService.verifyOtp(EMAIL, "123456");
        assertNull(expired.resetToken());
        assertEquals("Invalid or expired OTP", expired.message());
    }

    @Test
    void resendCooldownPreventsSendingAnotherCodeImmediately() {
        User user = createUser();
        PasswordResetChallenge challenge = new PasswordResetChallenge(user);
        challenge.setLastRequestedAt(LocalDateTime.now());
        challenge.setRequestWindowStartedAt(LocalDateTime.now());
        challenge.setRequestsInWindow(1);
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.of(challenge));

        passwordResetService.generateAndSendOtp(EMAIL);

        verifyNoInteractions(emailService);
        verify(challengeRepository, never())
                .saveAndFlush(any(PasswordResetChallenge.class));
    }

    @Test
    void hourlyRequestLimitPreventsSendingMoreThanThreeCodes() {
        User user = createUser();
        PasswordResetChallenge challenge = new PasswordResetChallenge(user);
        challenge.setLastRequestedAt(LocalDateTime.now().minusSeconds(61));
        challenge.setRequestWindowStartedAt(LocalDateTime.now().minusMinutes(30));
        challenge.setRequestsInWindow(3);
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user));
        when(challengeRepository.findByUserIdForUpdate(user.getId()))
                .thenReturn(Optional.of(challenge));

        passwordResetService.generateAndSendOtp(EMAIL);

        verifyNoInteractions(emailService);
        verify(challengeRepository, never())
                .saveAndFlush(any(PasswordResetChallenge.class));
    }

    private User createUser() {
        User user = new User();
        user.setId(42L);
        user.setName("Member");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode("OldPassword1!"));
        return user;
    }
}
