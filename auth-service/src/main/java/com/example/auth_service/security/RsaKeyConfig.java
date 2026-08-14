package com.example.auth_service.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the RSA key pair used to sign access tokens (RS256).
 * <p>
 * auth-service is the only service that ever sees the private key - it is
 * the sole token issuer. api-gateway is handed the public key only, which
 * is enough for it to verify a token's signature but not to mint new ones.
 * This is the actual point of moving off the old shared-HMAC-secret scheme:
 * compromising the gateway (or any downstream service) no longer hands an
 * attacker the ability to forge tokens.
 * <p>
 * Keys are supplied as base64-encoded DER (PKCS8 for the private key, X509
 * for the public key) via RSA_PRIVATE_KEY / RSA_PUBLIC_KEY. The defaults
 * below are a real, working dev-only key pair checked into application.yaml
 * the same way jwt.secret used to be - replace both env vars in any shared
 * or production environment.
 */
@Component
public class RsaKeyConfig {

    @Value("${rsa.private-key}")
    private String privateKeyBase64;

    @Value("${rsa.public-key}")
    private String publicKeyBase64;

    @Getter
    private RSAPrivateKey privateKey;

    @Getter
    private RSAPublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
}
