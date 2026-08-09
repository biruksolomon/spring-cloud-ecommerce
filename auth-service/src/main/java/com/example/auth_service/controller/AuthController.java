package com.example.auth_service.controller;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
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