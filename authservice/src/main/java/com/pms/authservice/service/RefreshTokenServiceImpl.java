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
    public String createRefreshToken(String userEmail) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userEmail(userEmail)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    // -------------------------------------------------------------------------
    // Rotate  (validate → revoke old → issue new access + refresh tokens)
    // -------------------------------------------------------------------------
    //
    // noRollbackFor = InvalidRefreshTokenException.class
    //
    // WHY this is required:
    //   rotate() is @Transactional (REQUIRED). When a reuse attack is detected,
    //   revokeAllByUserEmail() issues a bulk UPDATE inside this transaction.
    //   Then InvalidRefreshTokenException (a RuntimeException) is thrown.
    //   Spring's default behaviour for @Transactional is to ROLLBACK on any
    //   RuntimeException — which rolls back the bulk UPDATE too, leaving the
    //   attacker's token family still active in the DB.
    //
    //   noRollbackFor tells Spring: "when THIS specific exception escapes,
    //   commit what has already run instead of rolling it back."
    //   The revocation UPDATE commits, the exception still propagates to the
    //   caller, and the HTTP layer returns 401 — exactly the correct outcome.

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenResponse rotate(RefreshTokenRequest request) {

        String incomingToken = request.getRefreshToken();

        RefreshToken stored = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            // Reuse of a revoked token — possible replay attack.
            // Revoke the entire token family for this user with a single bulk UPDATE.
            log.warn("[RefreshToken] Reuse detected for user: {}. Revoking all tokens.",
                    stored.getUserEmail());
            int revoked = refreshTokenRepository.revokeAllByUserEmail(stored.getUserEmail());
            log.warn("[RefreshToken] Revoked {} tokens for user: {}", revoked,
                    stored.getUserEmail());
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

        String email = stored.getUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        String newAccessToken  = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = createRefreshToken(user.getEmail());

        log.debug("[RefreshToken] Rotated token for user: {}", email);

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
    public void revokeAll(String userEmail) {
        int revoked = refreshTokenRepository.revokeAllByUserEmail(userEmail);
        log.debug("[RefreshToken] Revoked {} tokens on logout for user: {}", revoked, userEmail);
    }
}