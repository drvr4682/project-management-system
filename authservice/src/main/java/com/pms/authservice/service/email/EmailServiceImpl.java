package com.pms.authservice.service.email;

import com.pms.authservice.exception.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String to, String name, String verificationLink) {
        log.info("[Email] Preparing plain text verification email for: {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, "PMS App Support");
            helper.setTo(to);
            helper.setSubject("PMS — Verify your email address");

            String body = "Hi " + name + ",\n\n"
                    + "Thank you for registering on PMS. Please click the link below to verify your email and activate your account:\n"
                    + verificationLink + "\n\n"
                    + "This verification link is valid for 24 hours.\n\n"
                    + "Best regards,\n"
                    + "PMS App Support";

            helper.setText(body, false); // false = plain text

            mailSender.send(message);
            log.info("[Email] Successfully sent verification email to: {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[Email] Error occurred while sending email to {}: {}", to, e.getMessage(), e);
            throw new EmailSendingException("Failed to send verification email: " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        log.info("[Email] Preparing plain text password reset email for: {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, "PMS App Support");
            helper.setTo(to);
            helper.setSubject("PMS — Reset your password");

            String body = "Hello " + name + ",\n\n"
                    + "Use the link below to reset your password:\n\n"
                    + resetLink + "\n\n"
                    + "This link expires in 30 minutes.\n\n"
                    + "Best regards,\n"
                    + "PMS App Support";

            helper.setText(body, false); // false = plain text

            mailSender.send(message);
            log.info("[Email] Successfully sent password reset email to: {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[Email] Error occurred while sending password reset email to {}: {}", to, e.getMessage(), e);
            throw new EmailSendingException("Failed to send password reset email: " + e.getMessage());
        }
    }
}
