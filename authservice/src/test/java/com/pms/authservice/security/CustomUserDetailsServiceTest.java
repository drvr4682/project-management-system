package com.pms.authservice.security;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(userRepository);

    @Test
    void shouldLoadUserByEmail() {

        User user = User.builder()
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        Mockito.when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("test@mail.com");

        assertEquals("test@mail.com", result.getUsername());
    }
}