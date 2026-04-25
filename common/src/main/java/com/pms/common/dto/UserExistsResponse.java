package com.pms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contract DTO — shared between Auth Service (producer) and
 * Project/Task Services (consumers via Feign).
 *
 * This is the ONLY place this class is defined.
 * Do NOT duplicate it inside individual services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {

    private String email;
    private boolean exists;

    /**
     * Static factory used by Auth Service when building a successful response.
     */
    public static UserExistsResponse found(String email) {
        return UserExistsResponse.builder()
                .email(email)
                .exists(true)
                .build();
    }

    /**
     * Static factory for a not-found response (used in tests and fallbacks).
     */
    public static UserExistsResponse notFound(String email) {
        return UserExistsResponse.builder()
                .email(email)
                .exists(false)
                .build();
    }
}