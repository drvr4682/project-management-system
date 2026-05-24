package com.pms.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {

    // New short-lived access token
    private String accessToken;

    // New refresh token (rotation: old one is revoked)
    private String refreshToken;

    private String email;
    private String role;
}