package com.pms.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;

    private String refreshToken;
    
    private String email;
    private String role;
    private java.util.UUID id;
    private String firstName;
    private String surname;
}