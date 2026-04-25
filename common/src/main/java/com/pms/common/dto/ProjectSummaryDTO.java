package com.pms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal contract DTO — used ONLY for inter-service communication.
 *
 * Task Service receives this when it calls Project Service via Feign.
 * It intentionally contains only the fields Task Service needs.
 *
 * Do NOT use this as the public API response in Project Service —
 * Project Service has its own ProjectResponseDTO for that purpose.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryDTO {

    private Long id;
    private String name;
    private String status;
    private String ownerId;
}