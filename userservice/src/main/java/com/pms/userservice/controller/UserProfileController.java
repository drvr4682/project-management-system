package com.pms.userservice.controller;

import com.pms.common.dto.UserProfileResponse;
import com.pms.common.dto.UserSearchResponse;
import com.pms.common.security.SecurityUtils;
import com.pms.userservice.dto.UserProfileCreationRequest;
import com.pms.userservice.dto.UserProfileUpdateRequest;
import com.pms.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID id) {
        log.info("Fetching profile for user ID: {}", id);
        return ResponseEntity.ok(userProfileService.getProfileById(id));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Fetching self profile for user ID: {}", currentUserId);
        return ResponseEntity.ok(userProfileService.getProfileMe(currentUserId));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/me")
    public ResponseEntity<UserProfileResponse> completeProfile(
            @Valid @RequestBody UserProfileCreationRequest request) {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Completing onboarding profile for user ID: {}", currentUserId);
        return ResponseEntity.ok(userProfileService.createProfile(currentUserId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Updating self profile for user ID: {}", currentUserId);
        return ResponseEntity.ok(userProfileService.updateProfile(currentUserId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<Page<UserSearchResponse>> searchProfiles(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("Searching profiles with query: '{}' | page: {} | size: {}", q, page, size);

        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        return ResponseEntity.ok(userProfileService.searchProfiles(q, pageable));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is healthy");
    }
}
