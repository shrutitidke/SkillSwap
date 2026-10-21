
package com.skillswap.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.username = username;
        this.password = password;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Password-reset email is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(username);
        message.setTo(toEmail);
        message.setSubject("SkillSwap - Password Reset OTP");
        message.setText(
                "Hello,\n\n" +
                        "Your SkillSwap password reset OTP is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n" +
                        "Do not share this OTP with anyone.\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Regards,\nSkillSwap Team"
        );

        mailSender.send(message);
    }
}
