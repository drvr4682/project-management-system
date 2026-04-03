package com.pms.authservice.service;

import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    boolean userExists(String email);
}