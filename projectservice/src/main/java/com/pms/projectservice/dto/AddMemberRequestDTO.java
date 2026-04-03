package com.pms.projectservice.dto;

import lombok.Data;

@Data
public class AddMemberRequestDTO {
    private String userId;
    private String role; //ADMIN / MEMBER / VIEWER
}
