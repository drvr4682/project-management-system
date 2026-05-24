package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.InvalidCredentialsException;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.repository.UserRepository;
import com.pms.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtil                jwtUtil;
    private final AuthenticationManager  authenticationManager;
    private final RefreshTokenService    refreshTokenService;
    private final StringRedisTemplate    redisTemplate;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + request.getEmail()
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);

        log.info("[Auth] Registered new user: {} | role: {}", savedUser.getEmail(), savedUser.getRole());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        String accessToken  = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        log.info("[Auth] Login successful: {}", user.getEmail());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        return refreshTokenService.rotate(request);
    }

    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void logout(String userEmail, String accessToken) {

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                if (jwtUtil.validateToken(accessToken)) {
                    String jti         = jwtUtil.extractJti(accessToken);
                    long   remainingMs = jwtUtil.getExpirationMillis(accessToken);

                    if (jti != null && remainingMs > 0) {
                        String redisKey = "blocklist:jti:" + jti;
                        redisTemplate.opsForValue()
                                .set(redisKey, "revoked", remainingMs, TimeUnit.MILLISECONDS);
                        log.info("[Auth] Access token blocklisted | user: {} | jti: {} | ttl: {}ms",
                                userEmail, jti, remainingMs);
                    }
                }
            } catch (Exception e) {
                log.warn("[Auth] Failed to blocklist access token for user {}: {}",
                        userEmail, e.getMessage());
            }
        }

        refreshTokenService.revokeAll(userEmail);

        log.info("[Auth] Logout complete for user: {}", userEmail);
    }
}