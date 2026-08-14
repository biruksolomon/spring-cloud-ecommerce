package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A rotatable, revocable refresh token.
 * <p>
 * The raw token value is never stored - only its SHA-256 hash (see
 * RefreshTokenService), so a leaked database dump doesn't hand out usable
 * tokens the way a leaked JWT secret would. Each refresh call rotates: the
 * presented token is revoked and a new one issued, so a stolen-and-reused
 * refresh token is detectable (the legitimate owner's next refresh will
 * fail because the token they hold was already revoked by the thief's use
 * of it, or vice versa).
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
