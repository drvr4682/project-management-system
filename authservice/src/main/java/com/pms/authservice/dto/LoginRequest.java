package com.pms.authservice.dto;

import com.pms.authservice.entity.Role;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}