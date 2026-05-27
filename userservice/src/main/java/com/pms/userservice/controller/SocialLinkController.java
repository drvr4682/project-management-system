package com.pms.userservice.controller;

import com.pms.common.dto.SocialLinkResponse;
import com.pms.common.security.SecurityUtils;
import com.pms.userservice.dto.SocialLinkRequest;
import com.pms.userservice.service.SocialLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/social-links")
@RequiredArgsConstructor
public class SocialLinkController {

    private final SocialLinkService socialLinkService;
    private final SecurityUtils securityUtils;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<SocialLinkResponse> addSocialLink(
            @Valid @RequestBody SocialLinkRequest request) {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Request to add social link for user ID: {}", currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(socialLinkService.addSocialLink(currentUserId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<SocialLinkResponse> updateSocialLink(
            @PathVariable UUID id,
            @Valid @RequestBody SocialLinkRequest request) {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Request to update social link ID: {} for user ID: {}", id, currentUserId);
        return ResponseEntity.ok(socialLinkService.updateSocialLink(currentUserId, id, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSocialLink(@PathVariable UUID id) {
        String currentUserId = securityUtils.getCurrentUser();
        log.info("Request to delete social link ID: {} for user ID: {}", id, currentUserId);
        socialLinkService.deleteSocialLink(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}
