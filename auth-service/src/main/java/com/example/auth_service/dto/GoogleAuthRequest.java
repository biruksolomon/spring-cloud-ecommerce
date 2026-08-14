package com.example.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the frontend sends to POST /auth/google - the raw Google ID token
 * it got back from Google's own Sign-In SDK. This service verifies it and
 * never talks to Google's redirect/authorization endpoints itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    @NotBlank(message = "idToken is required")
    private String idToken;
}