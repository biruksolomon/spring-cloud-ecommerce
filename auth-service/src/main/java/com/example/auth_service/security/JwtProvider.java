package com.example.auth_service.security;

import com.example.auth_service.entity.Role;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * Issues and verifies short-lived RS256 access tokens.
 * <p>
 * Signed with the private key from {@link RsaKeyConfig} - only auth-service
 * can mint a valid token. api-gateway verifies with the public key alone.
 * <p>
 * Access tokens are intentionally short-lived (15 min default). They are
 * NOT individually revocable - see {@link com.example.auth_service.service.RefreshTokenService}
 * for the revocable half of the pair. A stolen access token is only ever
 * useful for its remaining lifetime, which is the accepted trade-off for
 * not standing up a distributed token blacklist (e.g. Redis) for this
 * project's scope. If that changes, the "jti" claim below is already in
 * place to key a blacklist by.
 */
@Component
public class JwtProvider {

    private final RsaKeyConfig rsaKeyConfig;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    public JwtProvider(RsaKeyConfig rsaKeyConfig) {
        this.rsaKeyConfig = rsaKeyConfig;
    }

    public String generateAccessToken(Long userId, String email, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpirationMs))
                .signWith(rsaKeyConfig.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(rsaKeyConfig.getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}