package com.pms.authservice.service.password;

import com.pms.authservice.dto.ForgotPasswordRequest;
import com.pms.authservice.dto.ResetPasswordRequest;

public interface PasswordResetService {
    void handleForgotPassword(ForgotPasswordRequest request);
    void handleResetPassword(ResetPasswordRequest request);
}
