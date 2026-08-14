package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.GoogleAuthRequest;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.LogoutRequest;
import com.example.auth_service.dto.RefreshRequest;
import com.example.auth_service.dto.RefreshResponse;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.dto.UserResponse;
import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.UnauthorizedUserAccessException;
import com.example.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Sign in (or, on first use, sign up) with a Google ID token the
    // frontend already obtained from Google's own sign-in SDK. Returns the
    // exact same shape as /login and /register - an access + refresh token
    // pair - so the frontend treats this as just another way to arrive at
    // an authenticated session.
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    // Exchanges a refresh token for a new access token + a new (rotated)
    // refresh token. No Authorization header is required or checked here -
    // the refresh token itself is the credential, since the access token
    // that would normally prove identity has likely already expired by
    // the time a client needs to call this.
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    // Revokes one refresh token (the current session/device). Always
    // returns 204 whether or not the token was recognized, so this can't
    // be used to probe for valid tokens.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // Revokes every refresh token for the calling user - signs them out on
    // every device. Requires a valid access token (routed through the
    // gateway, which populates the "userId" attribute via UserContextFilter).
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest request) {
        Long callerId = (Long) request.getAttribute("userId");
        if (callerId == null) {
            throw new UnauthorizedUserAccessException(-1L);
        }
        authService.logoutAll(callerId);
        return ResponseEntity.noContent().build();
    }

    // Returns another user's profile - so unlike register/login, this one
    // has to check who's asking. Self-lookup is always allowed; looking up
    // someone else's account requires ADMIN. The response is a UserResponse
    // (no password hash), never the raw User entity.
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId, HttpServletRequest request) {
        Long callerId = (Long) request.getAttribute("userId");
        Role callerRole = (Role) request.getAttribute("userRole");

        boolean isSelf = callerId != null && callerId.equals(userId);
        boolean isAdmin = callerRole == Role.ADMIN;

        if (!isSelf && !isAdmin) {
            throw new UnauthorizedUserAccessException(userId);
        }

        User user = authService.getUserById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}