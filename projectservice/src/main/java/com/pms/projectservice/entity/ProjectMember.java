package com.pms.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "project_members",
        indexes = {
        @Index(name = "idx_member_project", columnList = "project_id"),
        @Index(name = "idx_member_user", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;
}