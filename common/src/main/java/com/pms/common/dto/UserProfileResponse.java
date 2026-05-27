package com.pms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String firstName;
    private String surname;
    private String username;
    private String bio;
    private String profileImageUrl;
    private String designation;
    private String timezone;
    private String statusMessage;
    private boolean active;
    private List<SocialLinkResponse> socialLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
