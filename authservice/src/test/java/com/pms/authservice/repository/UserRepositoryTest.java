package com.pms.authservice.repository;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = User.builder()
                .firstName("Exists")
                .surname("User")
                .email("test@example.com")
                .password("hashedpassword")
                .role(Role.USER)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void shouldReturnTrueIfEmailExists() {

        User user = User.builder()
                .firstName("Test")
                .surname("User")
                .email("exists@example.com")
                .password("hashedpassword")
                .role(Role.USER)
                .build();

        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void shouldReturnFalseIfEmailNotExists() {
        assertFalse(userRepository.existsByEmail("nobody@example.com"));
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownEmail() {
        Optional<User> found = userRepository.findByEmail("ghost@example.com");
        assertFalse(found.isPresent());
    }
}