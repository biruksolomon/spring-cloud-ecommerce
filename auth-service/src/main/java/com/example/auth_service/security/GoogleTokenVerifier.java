package com.example.auth_service.security;

import com.example.auth_service.exception.InvalidGoogleTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifies "Sign in with Google" ID tokens sent to POST /auth/google.
 * <p>
 * This is the entire OAuth2 surface auth-service has: it never redirects
 * anyone to Google and never sees a Google client secret. The frontend
 * (web or mobile) runs Google's own sign-in SDK, gets back a signed ID
 * token, and hands it to us. GoogleIdTokenVerifier checks the signature
 * against Google's published public keys, the expiry, the issuer, and
 * that the token was actually minted for our client ID (google.client-id
 * below) - the same trust model JwtProvider uses for our own tokens, just
 * verifying someone else's signature instead of our own.
 */
@Component
public class GoogleTokenVerifier {

    @Value("${google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (googleClientId == null || googleClientId.isBlank()) {
            // Left unconfigured on purpose in dev/test environments that
            // don't need Google sign-in. verify() below fails fast and
            // explicitly rather than silently trusting tokens meant for
            // no audience in particular.
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        if (verifier == null) {
            throw new IllegalStateException(
                    "google.client-id is not configured on auth-service - set GOOGLE_CLIENT_ID");
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException();
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException();
        }
    }
}