package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * register() is a write — needs @Transactional.
     * If the save fails after the existence check, no partial data is left.
     */
    @Transactional
    @Override
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already registered: " + email);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {}", email);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    /**
     * login() reads from the DB — readOnly = true improves performance.
     * authenticate() delegates to Spring Security — not a DB write.
     */
    @Transactional(readOnly = true)
    @Override
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("User logged in: {}", email);

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Pure read — readOnly = true.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase(Locale.ROOT));
    }
}