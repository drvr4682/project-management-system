package com.pms.authservice.service;

import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;

public interface RefreshTokenService {

    String createRefreshToken(java.util.UUID userId);

    RefreshTokenResponse rotate(RefreshTokenRequest request);

    void revokeAll(java.util.UUID userId);
}