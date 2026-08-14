package com.example.auth_service.service;

import com.example.auth_service.entity.RefreshToken;
import com.example.auth_service.exception.InvalidRefreshTokenException;
import com.example.auth_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues, rotates, and revokes refresh tokens.
 * <p>
 * The raw token handed to the client is a 256-bit random value, base64url
 * encoded. Only its SHA-256 hash is persisted (see RefreshToken). Rotation
 * on every use (rotate-and-revoke) means a refresh token is single-use in
 * practice - each successful /auth/refresh call returns a brand new token
 * and immediately revokes the one that was just presented.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Issues a brand new refresh token for a user, e.g. at login/register. */
    public String issue(Long userId) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a presented refresh token, revokes it, and issues its
     * replacement for the same user. Throws InvalidRefreshTokenException
     * if the token is unknown, expired, or already revoked (including a
     * reused, already-rotated-away token).
     */
    public RotationResult rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issue(existing.getUserId());
        return new RotationResult(existing.getUserId(), newRawToken);
    }

    /** Revokes a single refresh token, e.g. on /auth/logout. Silently no-ops if unknown. */
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /** Revokes every outstanding refresh token for a user, e.g. on /auth/logout-all or a password change. */
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM - this never happens.
            throw new IllegalStateException(e);
        }
    }

    public record RotationResult(Long userId, String newRawToken) {}
}
