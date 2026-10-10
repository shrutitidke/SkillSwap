
package com.skillswap.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("SkillSwap - Password Reset OTP");
        message.setText(
                "Hello,\n\n" +
                        "Your SkillSwap password reset OTP is: " + otp + "\n\n" +
                        "This OTP is valid for 10 minutes.\n" +
                        "Do not share this OTP with anyone.\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Regards,\nSkillSwap Team"
        );

        mailSender.send(message);
    }
}
