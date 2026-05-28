package com.pms.authservice.repository;

import com.pms.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, java.util.UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUserName(String email, String userName);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);
}