package com.pms.authservice.service.email;

public interface EmailService {
    void sendVerificationEmail(String to, String name, String verificationLink);
}
