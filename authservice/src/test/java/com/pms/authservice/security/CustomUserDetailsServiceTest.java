package com.pms.authservice.security;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void shouldLoadUserByEmail() {

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        Mockito.when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("test@mail.com");

        assertEquals("test@mail.com", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {

        Mockito.when(userRepository.findByEmail("missing@mail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@mail.com")
        );
    }

    @Test
    void shouldReturnDisabledUserDetailsWhenUserNotEnabled() {
        User user = User.builder()
                .id(1L)
                .email("disabled@mail.com")
                .password("hashed")
                .role(Role.USER)
                .enabled(false)
                .build();

        Mockito.when(userRepository.findByEmail("disabled@mail.com"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername("disabled@mail.com");

        assertFalse(result.isEnabled(), "User details must be disabled when user is not enabled");
    }
}