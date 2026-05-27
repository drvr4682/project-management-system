package com.pms.userservice.service;

import com.pms.common.dto.UserProfileResponse;
import com.pms.common.dto.UserSearchResponse;
import com.pms.common.security.SecurityUtils;
import com.pms.userservice.dto.UserProfileUpdateRequest;
import com.pms.userservice.entity.UserProfile;
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
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SocialLinkRepository socialLinkRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UserProfileService userProfileService;

    private static final String USER_ID_STR = "e5a31a61-9cbf-4bfb-b654-e67d4b9f36f1";
    private static final UUID USER_UUID = UUID.fromString(USER_ID_STR);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create default profile successfully")
    void shouldCreateDefaultProfileSuccessfully() {
        when(userProfileRepository.existsById(USER_UUID)).thenReturn(false);
        when(userProfileRepository.existsByUsernameIgnoreCase(any(String.class))).thenReturn(false);

        userProfileService.createDefaultProfile(USER_UUID, "John", "Doe");

        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
        verify(auditLogger, times(1)).log(eq("SYSTEM"), eq("CREATE_PROFILE"), eq(USER_ID_STR), any(String.class));
    }

    @Test
    @DisplayName("Should generate unique username on conflict during registration")
    void shouldGenerateUniqueUsernameOnConflict() {
        when(userProfileRepository.existsById(USER_UUID)).thenReturn(false);
        when(userProfileRepository.existsByUsernameIgnoreCase("johndoe")).thenReturn(true);
        when(userProfileRepository.existsByUsernameIgnoreCase("johndoe1")).thenReturn(false);

        userProfileService.createDefaultProfile(USER_UUID, "John", "Doe");

        verify(userProfileRepository, times(1)).save(argThat(profile -> "johndoe1".equals(profile.getUsername())));
    }

    @Test
    @DisplayName("Should return active profile by ID")
    void shouldReturnActiveProfileById() {
        UserProfile profile = UserProfile.builder()
                .id(USER_UUID)
                .firstName("John")
                .surname("Doe")
                .username("johndoe")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.of(profile));
        when(socialLinkRepository.findByProfileId(USER_UUID)).thenReturn(new ArrayList<>());

        UserProfileResponse response = userProfileService.getProfileById(USER_UUID);

        assertNotNull(response);
        assertEquals(USER_UUID, response.getId());
        assertEquals("johndoe", response.getUsername());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when profile not found")
    void shouldThrowExceptionWhenProfileNotFound() {
        when(userProfileRepository.findByIdAndActiveTrue(USER_UUID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getProfileById(USER_UUID));
    }

    @Test
    @DisplayName("Should update profile fields successfully")
    void shouldUpdateProfileSuccessfully() {
        UserProfile profile = UserProfile.builder()
                .id(USER_UUID)
                .firstName("John")
                .surname("Doe")
                .username("johndoe")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .firstName("Johnny")
                .surname("Doe")
                .bio("New Bio")
                .designation("Staff Architect")
                .build();

        when(userProfileRepository.findById(USER_UUID)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(socialLinkRepository.findByProfileId(USER_UUID)).thenReturn(new ArrayList<>());

        UserProfileResponse response = userProfileService.updateProfile(USER_ID_STR, request);

        assertNotNull(response);
        assertEquals("Johnny", response.getFirstName());
        assertEquals("New Bio", response.getBio());
        assertEquals("Staff Architect", response.getDesignation());
    }

    @Test
    @DisplayName("Should search profiles successfully")
    void shouldSearchProfilesSuccessfully() {
        UserProfile profile = UserProfile.builder()
                .id(USER_UUID)
                .firstName("John")
                .surname("Doe")
                .username("johndoe")
                .active(true)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserProfile> pagedResult = new PageImpl<>(List.of(profile));

        when(userProfileRepository.searchActiveProfiles("jo", pageable)).thenReturn(pagedResult);

        Page<UserSearchResponse> searchResult = userProfileService.searchProfiles("jo", pageable);

        assertNotNull(searchResult);
        assertEquals(1, searchResult.getTotalElements());
        assertEquals("johndoe", searchResult.getContent().get(0).getUsername());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when search query too short")
    void shouldThrowExceptionWhenQueryTooShort() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(IllegalArgumentException.class, () -> userProfileService.searchProfiles("j", pageable));
    }
}
