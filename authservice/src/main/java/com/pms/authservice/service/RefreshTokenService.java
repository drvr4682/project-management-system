package com.pms.authservice.service;

import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;

public interface RefreshTokenService {

    /**
     * Create and persist a new refresh token for the given user email.
     * Called by AuthService after successful login.
     */
    String createRefreshToken(String userEmail);

    /**
     * Validate the incoming refresh token, rotate it (revoke old, issue new),
     * and return a fresh access token + new refresh token.
     * On detected reuse of a revoked token, revokes ALL tokens for the user.
     */
    RefreshTokenResponse rotate(RefreshTokenRequest request);

    /**
     * Revoke all active refresh tokens for a user.
     * Called on logout.
     */
    void revokeAll(String userEmail);
}