package com.pms.authservice.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String userName, String token);

    void sendResendVerificationEmail(String toEmail, String userName, String token);
}