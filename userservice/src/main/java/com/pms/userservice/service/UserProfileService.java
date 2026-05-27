package com.pms.userservice.service;

import com.pms.common.dto.SocialLinkResponse;
import com.pms.common.dto.UserProfileResponse;
import com.pms.common.dto.UserSearchResponse;
import com.pms.userservice.dto.UserProfileUpdateRequest;
import com.pms.userservice.entity.SocialLink;
import com.pms.userservice.entity.UserProfile;
import com.pms.userservice.exception.ResourceNotFoundException;
import com.pms.userservice.repository.SocialLinkRepository;
import com.pms.userservice.repository.UserProfileRepository;
import com.pms.userservice.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final SocialLinkRepository socialLinkRepository;
    private final AuditLogger auditLogger;

    @Transactional
    public void createDefaultProfile(UUID id, String firstName, String surname) {
        log.info("Creating default profile for user ID: {} | Name: {} {}", id, firstName, surname);

        if (userProfileRepository.existsById(id)) {
            log.warn("Profile already exists for user ID: {}", id);
            return;
        }

        // Generate unique, valid username
        String baseUsername = (firstName + (surname != null ? surname : "")).replaceAll("\\s+", "").toLowerCase();
        if (baseUsername.length() < 3) {
            baseUsername = "user" + baseUsername;
        }
        
        // Strip out non-alphanumeric characters just in case
        baseUsername = baseUsername.replaceAll("[^a-z0-9]", "");
        if (baseUsername.isEmpty()) {
            baseUsername = "user" + id.toString().substring(0, 5);
        }

        String finalUsername = baseUsername;
        int suffix = 1;
        while (userProfileRepository.existsByUsernameIgnoreCase(finalUsername)) {
            finalUsername = baseUsername + suffix++;
        }

        UserProfile profile = UserProfile.builder()
                .id(id)
                .firstName(firstName)
                .surname(surname)
                .username(finalUsername)
                .active(true)
                .build();

        userProfileRepository.save(profile);
        auditLogger.log("SYSTEM", "CREATE_PROFILE", id.toString(), "Default profile created with username: " + finalUsername);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID id) {
        UserProfile profile = userProfileRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + id));

        List<SocialLink> links = socialLinkRepository.findByProfileId(id);

        return mapToProfileResponse(profile, links);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileMe(String userIdStr) {
        UUID userId = parseUUID(userIdStr);
        return getProfileById(userId);
    }

    @Transactional
    public UserProfileResponse updateProfile(String userIdStr, UserProfileUpdateRequest request) {
        UUID userId = parseUUID(userIdStr);
        UserProfile profile = userProfileRepository.findById(userId)
                .filter(UserProfile::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        log.info("Updating profile for user ID: {}", userId);

        profile.setFirstName(request.getFirstName());
        profile.setSurname(request.getSurname());
        profile.setBio(request.getBio());
        profile.setProfileImageUrl(request.getProfileImageUrl());
        profile.setDesignation(request.getDesignation());
        profile.setTimezone(request.getTimezone());
        profile.setStatusMessage(request.getStatusMessage());

        UserProfile saved = userProfileRepository.save(profile);

        List<SocialLink> links = socialLinkRepository.findByProfileId(userId);

        auditLogger.log(userIdStr, "UPDATE_PROFILE", userId.toString(), "Profile fields updated successfully");

        return mapToProfileResponse(saved, links);
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchProfiles(String query, Pageable pageable) {
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters");
        }

        String cleanedQuery = query.trim();
        Page<UserProfile> profilesPage = userProfileRepository.searchActiveProfiles(cleanedQuery, pageable);

        return profilesPage.map(this::mapToSearchResponse);
    }

    private UUID parseUUID(String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidStr);
        }
    }

    private UserProfileResponse mapToProfileResponse(UserProfile profile, List<SocialLink> links) {
        List<SocialLinkResponse> linkResponses = links.stream()
                .map(link -> SocialLinkResponse.builder()
                        .id(link.getId())
                        .profileId(link.getProfileId())
                        .platform(link.getPlatform())
                        .url(link.getUrl())
                        .build())
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .surname(profile.getSurname())
                .username(profile.getUsername())
                .bio(profile.getBio())
                .profileImageUrl(profile.getProfileImageUrl())
                .designation(profile.getDesignation())
                .timezone(profile.getTimezone())
                .statusMessage(profile.getStatusMessage())
                .active(profile.isActive())
                .socialLinks(linkResponses)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private UserSearchResponse mapToSearchResponse(UserProfile profile) {
        return UserSearchResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .surname(profile.getSurname())
                .username(profile.getUsername())
                .designation(profile.getDesignation())
                .profileImageUrl(profile.getProfileImageUrl())
                .build();
    }
}
