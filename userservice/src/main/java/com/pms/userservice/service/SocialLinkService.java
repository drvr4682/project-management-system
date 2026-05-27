package com.pms.userservice.service;

import com.pms.common.dto.SocialLinkResponse;
import com.pms.userservice.dto.SocialLinkRequest;
import com.pms.userservice.entity.SocialLink;
import com.pms.userservice.entity.UserProfile;
import com.pms.userservice.exception.AccessDeniedException;
import com.pms.userservice.exception.ResourceNotFoundException;
import com.pms.userservice.repository.SocialLinkRepository;
import com.pms.userservice.repository.UserProfileRepository;
import com.pms.userservice.util.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLinkService {

    private final SocialLinkRepository socialLinkRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditLogger auditLogger;

    @Transactional
    public SocialLinkResponse addSocialLink(String userIdStr, SocialLinkRequest request) {
        UUID userId = parseUUID(userIdStr);
        
        UserProfile profile = userProfileRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        log.info("Adding social link for user ID: {} | Platform: {}", userId, request.getPlatform());

        SocialLink link = SocialLink.builder()
                .profileId(userId)
                .platform(request.getPlatform())
                .url(request.getUrl())
                .build();

        SocialLink saved = socialLinkRepository.save(link);

        auditLogger.log(userIdStr, "ADD_SOCIAL_LINK", userId.toString(), "Added platform: " + request.getPlatform());

        return mapToResponse(saved);
    }

    @Transactional
    public SocialLinkResponse updateSocialLink(String userIdStr, UUID linkId, SocialLinkRequest request) {
        UUID userId = parseUUID(userIdStr);

        UserProfile profile = userProfileRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        SocialLink link = socialLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found for ID: " + linkId));

        if (!link.getProfileId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to modify this social link");
        }

        log.info("Updating social link ID: {} | Platform: {}", linkId, request.getPlatform());

        link.setPlatform(request.getPlatform());
        link.setUrl(request.getUrl());

        SocialLink saved = socialLinkRepository.save(link);

        auditLogger.log(userIdStr, "UPDATE_SOCIAL_LINK", userId.toString(), "Updated platform: " + request.getPlatform());

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteSocialLink(String userIdStr, UUID linkId) {
        UUID userId = parseUUID(uuidStr(userIdStr));

        UserProfile profile = userProfileRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        SocialLink link = socialLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Social link not found for ID: " + linkId));

        if (!link.getProfileId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to delete this social link");
        }

        log.info("Deleting social link ID: {}", linkId);

        socialLinkRepository.delete(link);

        auditLogger.log(userIdStr, "DELETE_SOCIAL_LINK", userId.toString(), "Deleted platform: " + link.getPlatform());
    }

    private UUID parseUUID(String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidStr);
        }
    }

    private String uuidStr(String str) {
        return str;
    }

    private SocialLinkResponse mapToResponse(SocialLink link) {
        return SocialLinkResponse.builder()
                .id(link.getId())
                .profileId(link.getProfileId())
                .platform(link.getPlatform())
                .url(link.getUrl())
                .build();
    }
}
