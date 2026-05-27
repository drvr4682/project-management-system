package com.pms.authservice.service;

import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.entity.RefreshToken;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.InvalidRefreshTokenException;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.repository.RefreshTokenRepository;
import com.pms.authservice.repository.UserRepository;
import com.pms.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public String createRefreshToken(java.util.UUID userId) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    // -------------------------------------------------------------------------
    // Rotate  (validate → revoke old → issue new access + refresh tokens)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenResponse rotate(RefreshTokenRequest request) {

        String incomingToken = request.getRefreshToken();

        RefreshToken stored = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            log.warn("[RefreshToken] Reuse detected for user: {}. Revoking all tokens.",
                    stored.getUserId());
            int revoked = refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            log.warn("[RefreshToken] Revoked {} tokens for user: {}", revoked,
                    stored.getUserId());
            throw new InvalidRefreshTokenException(
                    "Refresh token has already been used or revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.saveAndFlush(stored);
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        // Normal rotation — revoke the used token then issue a new one
        stored.setRevoked(true);
        refreshTokenRepository.saveAndFlush(stored);

        java.util.UUID userId = stored.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        String newAccessToken  = jwtUtil.generateToken(user.getId().toString(), user.getRole().name());
        String newRefreshToken = createRefreshToken(user.getId());

        log.debug("[RefreshToken] Rotated token for user: {}", userId);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // -------------------------------------------------------------------------
    // Revoke all  (logout)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void revokeAll(java.util.UUID userId) {
        int revoked = refreshTokenRepository.revokeAllByUserId(userId);
        log.debug("[RefreshToken] Revoked {} tokens on logout for user: {}", revoked, userId);
    }
}