package com.pms.userservice.service;

import com.pms.common.dto.SocialLinkResponse;
import com.pms.common.dto.UserProfileResponse;
import com.pms.common.dto.UserSearchResponse;
import com.pms.userservice.client.AuthFeignClient;
import com.pms.userservice.dto.InternalUserDto;
import com.pms.userservice.dto.UserProfileCreationRequest;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final SocialLinkRepository socialLinkRepository;
    private final AuditLogger auditLogger;
    private final AuthFeignClient authFeignClient;

    @Transactional
    public void createDefaultProfile(UUID id, String firstName, String surname) {
        log.info("Creating default profile for user ID: {} | Name: {} {}", id, firstName, surname);

        if (userProfileRepository.existsById(id)) {
            log.warn("Profile already exists for user ID: {}", id);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .id(id)
                .firstName(firstName)
                .surname(surname)
                .active(true)
                .build();

        userProfileRepository.save(profile);
        auditLogger.log("SYSTEM", "CREATE_PROFILE", id.toString(), "Default profile created");
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
        return userProfileRepository.findByIdAndActiveTrue(userId)
                .map(profile -> {
                    List<SocialLink> links = socialLinkRepository.findByProfileId(userId);
                    return mapToProfileResponse(profile, links);
                })
                .orElseGet(() -> UserProfileResponse.builder()
                        .profileCompleted(false)
                        .build());
    }

    @Transactional
    public UserProfileResponse createProfile(String userIdStr, UserProfileCreationRequest request) {
        UUID userId = parseUUID(userIdStr);
        log.info("Creating profile for user ID: {} | Name: {} {}", userId, request.getFirstName(), request.getSurname());

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .id(userId)
                            .firstName(request.getFirstName())
                            .surname(request.getSurname())
                            .active(true)
                            .build();
                    UserProfile saved = userProfileRepository.save(newProfile);
                    auditLogger.log(userIdStr, "CREATE_PROFILE", userId.toString(), "Profile completed successfully");
                    return saved;
                });

        List<SocialLink> links = socialLinkRepository.findByProfileId(userId);
        return mapToProfileResponse(profile, links);
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

        List<UserProfile> profiles = profilesPage.getContent();
        List<UUID> ids = profiles.stream().map(UserProfile::getId).collect(Collectors.toList());
        Map<UUID, String> usernameMap = new java.util.HashMap<>();

        if (!ids.isEmpty()) {
            try {
                Map<UUID, String> resolved = authFeignClient.getBulkUsernames(ids);
                if (resolved != null) {
                    usernameMap.putAll(resolved);
                }
            } catch (Exception e) {
                log.warn("Failed to bulk resolve usernames for search results via AuthFeignClient: {}", e.getMessage());
            }
        }

        return profilesPage.map(profile -> {
            String resolvedUsername = usernameMap.get(profile.getId());
            return mapToSearchResponse(profile, resolvedUsername);
        });
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

        String resolvedUsername = null;
        try {
            InternalUserDto internalUser = authFeignClient.getUserInfo(profile.getId());
            if (internalUser != null) {
                resolvedUsername = internalUser.getUserName();
            }
        } catch (Exception e) {
            log.warn("Failed to dynamically resolve username for ID: {} via AuthFeignClient: {}", profile.getId(), e.getMessage());
        }

        return UserProfileResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .surname(profile.getSurname())
                .username(resolvedUsername)
                .bio(profile.getBio())
                .profileImageUrl(profile.getProfileImageUrl())
                .designation(profile.getDesignation())
                .timezone(profile.getTimezone())
                .statusMessage(profile.getStatusMessage())
                .active(profile.isActive())
                .socialLinks(linkResponses)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .profileCompleted(true)
                .build();
    }

    private UserSearchResponse mapToSearchResponse(UserProfile profile, String username) {
        return UserSearchResponse.builder()
                .id(profile.getId())
                .firstName(profile.getFirstName())
                .surname(profile.getSurname())
                .username(username)
                .designation(profile.getDesignation())
                .profileImageUrl(profile.getProfileImageUrl())
                .build();
    }
}
