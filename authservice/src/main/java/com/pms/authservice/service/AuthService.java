package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    RefreshTokenResponse refresh(RefreshTokenRequest request);
    boolean userExists(String email);
    void logout(String userEmail, String accessToken);
}