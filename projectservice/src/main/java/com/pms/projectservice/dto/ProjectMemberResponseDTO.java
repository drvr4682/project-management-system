package com.pms.projectservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectMemberResponseDTO {
    private String userId;
    private String role;
}