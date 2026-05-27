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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SocialLinkServiceTest {

    @Mock
    private SocialLinkRepository socialLinkRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private SocialLinkService socialLinkService;

    private static final String USER_ID_STR = "e5a31a61-9cbf-4bfb-b654-e67d4b9f36f1";
    private static final UUID USER_UUID = UUID.fromString(USER_ID_STR);
    private static final UUID LINK_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should add social link successfully for active profile")
    void shouldAddSocialLinkSuccessfully() {
        UserProfile profile = UserProfile.builder().id(USER_UUID).active(true).build();
        SocialLinkRequest request = SocialLinkRequest.builder().platform("Github").url("https://github.com/johndoe").build();
        SocialLink savedLink = SocialLink.builder().id(LINK_UUID).profileId(USER_UUID).platform("Github").url("https://github.com/johndoe").build();

        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.of(profile));
        when(socialLinkRepository.save(any(SocialLink.class))).thenReturn(savedLink);

        SocialLinkResponse response = socialLinkService.addSocialLink(USER_ID_STR, request);

        assertNotNull(response);
        assertEquals("Github", response.getPlatform());
        assertEquals("https://github.com/johndoe", response.getUrl());
        verify(socialLinkRepository, times(1)).save(any(SocialLink.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when adding social link for inactive profile")
    void shouldThrowExceptionWhenProfileNotFoundForAdd() {
        SocialLinkRequest request = SocialLinkRequest.builder().platform("Github").url("https://github.com/johndoe").build();
        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> socialLinkService.addSocialLink(USER_ID_STR, request));
    }

    @Test
    @DisplayName("Should update social link owned by user successfully")
    void shouldUpdateSocialLinkSuccessfully() {
        UserProfile profile = UserProfile.builder().id(USER_UUID).active(true).build();
        SocialLinkRequest request = SocialLinkRequest.builder().platform("LinkedIn").url("https://linkedin.com/in/johndoe").build();
        SocialLink existingLink = SocialLink.builder().id(LINK_UUID).profileId(USER_UUID).platform("Github").url("https://github.com/johndoe").build();

        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.of(profile));
        when(socialLinkRepository.findById(LINK_UUID)).thenReturn(Optional.of(existingLink));
        when(socialLinkRepository.save(any(SocialLink.class))).thenReturn(existingLink);

        SocialLinkResponse response = socialLinkService.updateSocialLink(USER_ID_STR, LINK_UUID, request);

        assertNotNull(response);
        assertEquals("LinkedIn", response.getPlatform());
        assertEquals("https://linkedin.com/in/johndoe", response.getUrl());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when updating social link owned by someone else")
    void shouldThrowAccessDeniedOnUpdateForOtherUser() {
        UserProfile profile = UserProfile.builder().id(USER_UUID).active(true).build();
        SocialLinkRequest request = SocialLinkRequest.builder().platform("LinkedIn").url("https://linkedin.com/in/johndoe").build();
        SocialLink existingLink = SocialLink.builder().id(LINK_UUID).profileId(UUID.randomUUID()).platform("Github").url("https://github.com/johndoe").build();

        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.of(profile));
        when(socialLinkRepository.findById(LINK_UUID)).thenReturn(Optional.of(existingLink));

        assertThrows(AccessDeniedException.class, () -> socialLinkService.updateSocialLink(USER_ID_STR, LINK_UUID, request));
    }

    @Test
    @DisplayName("Should delete social link owned by user successfully")
    void shouldDeleteSocialLinkSuccessfully() {
        UserProfile profile = UserProfile.builder().id(USER_UUID).active(true).build();
        SocialLink existingLink = SocialLink.builder().id(LINK_UUID).profileId(USER_UUID).platform("Github").url("https://github.com/johndoe").build();

        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.of(profile));
        when(socialLinkRepository.findById(LINK_UUID)).thenReturn(Optional.of(existingLink));

        socialLinkService.deleteSocialLink(USER_ID_STR, LINK_UUID);

        verify(socialLinkRepository, times(1)).delete(existingLink);
    }
}
