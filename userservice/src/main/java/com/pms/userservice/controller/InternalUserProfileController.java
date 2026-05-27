package com.pms.userservice.controller;

import com.pms.userservice.dto.InternalProfileCreationRequest;
import com.pms.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/profile")
    public ResponseEntity<Void> createProfile(@RequestBody InternalProfileCreationRequest request) {
        log.info("Internal request to create profile for user ID: {}", request.getId());
        userProfileService.createDefaultProfile(request.getId(), request.getFirstName(), request.getSurname());
        return ResponseEntity.ok().build();
    }
}
