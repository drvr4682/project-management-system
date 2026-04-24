package com.pms.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {

    private String email;
    private boolean exists;

    // Static factory — clean and readable at call site
    public static UserExistsResponse found(String email) {
        return UserExistsResponse.builder()
                .email(email)
                .exists(true)
                .build();
    }
}