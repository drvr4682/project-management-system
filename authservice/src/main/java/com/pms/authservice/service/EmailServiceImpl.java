package com.pms.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    // JavaMailSender only — Thymeleaf removed. All HTML is built inline.
    // If template complexity grows, extract to a separate TemplateBuilder class
    // rather than re-adding a template engine dependency.
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    // =========================================================================
    // VERIFICATION EMAIL  (initial send)
    // =========================================================================

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String userName, String token) {
        String subject   = "PMS — Verify your email address";
        String verifyUrl = buildVerifyUrl(token);
        String body      = buildVerificationHtml(userName, verifyUrl, false);
        send(toEmail, subject, body);
        log.info("[Mail] Verification email queued → {}", toEmail);
    }

    // =========================================================================
    // VERIFICATION EMAIL  (resend)
    // =========================================================================

    @Async
    @Override
    public void sendResendVerificationEmail(String toEmail, String userName, String token) {
        String subject   = "PMS — New verification link";
        String verifyUrl = buildVerifyUrl(token);
        String body      = buildVerificationHtml(userName, verifyUrl, true);
        send(toEmail, subject, body);
        log.info("[Mail] Resend verification email queued → {}", toEmail);
    }

    // =========================================================================
    // URL BUILDERS
    // =========================================================================

    private String buildVerifyUrl(String token) {
        // Routes through the API Gateway → forwarded to authservice
        return baseUrl + "/api/v1/auth/verify-email?token=" + token;
    }

    // =========================================================================
    // HTML BUILDER
    // Plain HTML string — no Thymeleaf, no external template files.
    // Requirement explicitly states plain-text / simple email only.
    // =========================================================================

    private String buildVerificationHtml(String userName, String verifyUrl, boolean isResend) {

        String heading = isResend
                ? "Here is your new verification link"
                : "Welcome to PMS! Please verify your email";

        String intro = isResend
                ? "You requested a new verification link. Click the button below to verify your email address."
                : "Thank you for registering. Click the button below to activate your account.";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:30px'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:8px;"
                + "padding:32px;box-shadow:0 2px 8px rgba(0,0,0,.08)'>"
                + "<h2 style='color:#2c3e50;margin-top:0'>" + escapeHtml(heading) + "</h2>"
                + "<p style='color:#555'>Hi <strong>" + escapeHtml(userName) + "</strong>,</p>"
                + "<p style='color:#555'>" + escapeHtml(intro) + "</p>"
                + "<div style='text-align:center;margin:32px 0'>"
                + "<a href='" + verifyUrl + "' "
                + "style='background:#27ae60;color:#fff;padding:14px 28px;border-radius:5px;"
                + "text-decoration:none;font-size:15px;display:inline-block'>Verify Email Address</a>"
                + "</div>"
                + "<p style='color:#888;font-size:13px'>This link expires in <strong>24 hours</strong>. "
                + "If you did not create an account, you can safely ignore this email.</p>"
                + "<hr style='border:none;border-top:1px solid #eee;margin:24px 0'/>"
                + "<p style='color:#bbb;font-size:11px;text-align:center'>PMS — Project Management System<br/>"
                + "This is an automated message, please do not reply.</p>"
                + "</div></body></html>";
    }

    // =========================================================================
    // INTERNAL SEND
    // =========================================================================

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);   // true = HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            // Mail failure must NOT roll back any business transaction
            log.error("[Mail] Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    // =========================================================================
    // MINIMAL HTML ESCAPING — prevents XSS if any input contains HTML chars
    // =========================================================================

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}