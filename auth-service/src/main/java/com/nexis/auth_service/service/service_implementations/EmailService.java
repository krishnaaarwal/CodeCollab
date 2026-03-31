package com.nexis.auth_service.service.service_implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;  // it will inject in runtime so its fine giving error

    @Async
    public void sendPasswordResetEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@nexis-ide.com"); // This is just a display name
            message.setTo(toEmail);
            message.setSubject("Nexis IDE - Your Password Reset Code");
            message.setText("Hello,\n\nYou requested a password reset for your Nexis account.\n\n" +
                    "Your 6-digit OTP is: " + otp + "\n\n" +
                    "This code will expire in 10 minutes. If you did not request this, please ignore this email.\n\n" +
                    "Thanks,\nThe Nexis Team");

            mailSender.send(message);
            log.info("✅ Real email successfully sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", toEmail, e);
        }
    }
}
