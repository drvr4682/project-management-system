package com.pms.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_first_name", columnList = "first_name"),
        @Index(name = "idx_surname", columnList = "surname")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "surname", length = 100)
    private String surname;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(length = 1000)
    private String bio;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(length = 150)
    private String designation;

    @Column(length = 50)
    private String timezone;

    @Column(name = "status_message", length = 255)
    private String statusMessage;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.username != null) {
            this.username = this.username.trim().toLowerCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.username != null) {
            this.username = this.username.trim().toLowerCase();
        }
    }
}
