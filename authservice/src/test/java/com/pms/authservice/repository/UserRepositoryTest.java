package com.pms.authservice.repository;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void shouldReturnTrueIfEmailExists() {

        User user = User.builder()
                .name("Test User")
                .email("exists@example.com")
                .password("password")
                .role(Role.USER)
                .build();

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("exists@example.com");

        assertTrue(exists);
    }
}