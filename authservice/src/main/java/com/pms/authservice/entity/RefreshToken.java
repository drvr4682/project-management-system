package com.pms.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Opaque random token stored as plain UUID string
    @Column(nullable = false, unique = true)
    private String token;

    // Owning user — stored as UUID
    @Column(nullable = false, columnDefinition = "UUID")
    private java.util.UUID userId;

    @Column(nullable = false)
    private Instant expiresAt;

    // Soft-revoke flag; set to true on logout or token rotation
    @Column(nullable = false)
    private boolean revoked;
}