package com.pms.authservice.service;

import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
}