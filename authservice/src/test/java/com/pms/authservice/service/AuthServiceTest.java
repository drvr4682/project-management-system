package com.pms.authservice.service;

import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);

    private final AuthService authService =
            new AuthServiceImpl(userRepository, passwordEncoder);

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@mail.com");
        request.setPassword("123");
        request.setRole(Role.USER);

        // Mock behavior
        Mockito.when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        Mockito.when(passwordEncoder.encode("123")).thenReturn("hashed");

        User savedUser = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(savedUser);

        // Execute
        var response = authService.register(request);

        // Verify
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@mail.com", response.getEmail());
    }
}