package com.pms.projectservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String owner;
}