package com.pms.authservice.service;

import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;

public interface RefreshTokenService {

    String createRefreshToken(String userEmail);

    RefreshTokenResponse rotate(RefreshTokenRequest request);

    void revokeAll(String userEmail);
}