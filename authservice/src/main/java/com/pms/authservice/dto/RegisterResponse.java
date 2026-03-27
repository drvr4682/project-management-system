package com.pms.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
}