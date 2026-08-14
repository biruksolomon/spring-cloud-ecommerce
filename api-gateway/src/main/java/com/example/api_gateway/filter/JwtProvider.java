package com.example.api_gateway.filter;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies access tokens issued by auth-service.
 * <p>
 * Only the RSA public key is configured here (RSA_PUBLIC_KEY) - the
 * gateway can verify a token's signature but has no way to mint one. This
 * replaces the old scheme where gateway and auth-service shared the same
 * HMAC secret, which meant compromising either service was enough to
 * forge tokens for the whole platform.
 */
@Component
public class JwtProvider {

    @Value("${rsa.public-key}")
    private String publicKeyBase64;

    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    public Long extractUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
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
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}