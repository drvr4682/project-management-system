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
import org.springframework.transaction.annotation.Propagation;
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

    // REQUIRES_NEW: always runs in its own independent transaction that commits
    // before returning to the caller, regardless of any outer transaction.
    //
    // Why this matters:
    //   rotate() is @Transactional (REQUIRED). When rotate() calls createRefreshToken()
    //   with plain REQUIRED propagation, the new token is saved but NOT committed
    //   until rotate()'s outer transaction commits — which is fine for the normal
    //   rotation path.
    //
    //   But for the reuse-attack path (Step 3 in the test):
    //   - Step 2's rotate() called createRefreshToken() and committed the new token.
    //   - Step 3's rotate() calls revokeAllByUserEmail() (bulk UPDATE WHERE revoked=false).
    //   - With REQUIRED propagation, if Step 2's new token ended up being seen by
    //     Step 3's session only through the connection pool (same H2 in-memory
    //     connection reuse), the bulk UPDATE misses it — Revoked 0.
    //
    //   With REQUIRES_NEW, createRefreshToken() always commits independently.
    //   The new token is a fully committed, durable row before rotate() continues.
    //   Any subsequent bulk UPDATE in any transaction will see it.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @Override
    @Transactional
    public RefreshTokenResponse rotate(RefreshTokenRequest request) {

        String incomingToken = request.getRefreshToken();

        RefreshToken stored = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (stored.isRevoked()) {
            // Reuse of a revoked token — possible replay attack.
            // Revoke the entire active token family with a single bulk UPDATE.
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

        // Normal rotation — revoke the used token, issue a new one
        stored.setRevoked(true);
        refreshTokenRepository.saveAndFlush(stored);

        String email = stored.getUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        String newAccessToken  = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        // createRefreshToken uses REQUIRES_NEW — commits independently before returning
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