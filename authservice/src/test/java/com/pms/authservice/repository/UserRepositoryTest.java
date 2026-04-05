package com.pms.authservice.repository;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {

        User user = new User();
        user.setName("Exists User");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
    }

    @Test
    void shouldReturnTrueIfEmailExists() {

        User user = new User();
        user.setName("Test User");
        user.setEmail("exists@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);

        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("exists@example.com");

        assertTrue(exists);
    }
}