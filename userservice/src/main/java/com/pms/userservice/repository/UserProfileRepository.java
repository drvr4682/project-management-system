package com.pms.userservice.repository;

import com.pms.userservice.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByIdAndActiveTrue(UUID id);

    Optional<UserProfile> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("SELECT u FROM UserProfile u WHERE u.active = true AND (" +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.surname) LIKE LOWER(CONCAT('%', :q, '%'))" +
           ") ORDER BY " +
           "CASE WHEN LOWER(u.username) = LOWER(:q) THEN 0 ELSE 1 END ASC, " +
           "CASE WHEN LOWER(u.firstName) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END ASC, " +
           "u.username ASC")
    Page<UserProfile> searchActiveProfiles(@Param("q") String q, Pageable pageable);
}
