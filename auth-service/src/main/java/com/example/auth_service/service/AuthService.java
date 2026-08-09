package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.AccountInactiveException;
import com.example.auth_service.exception.EmailAlreadyRegisteredException;
import com.example.auth_service.exception.InvalidCredentialsException;
import com.example.auth_service.exception.UserNotFoundException;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .active(true)
                // Registration always creates a regular customer account -
                // admin accounts are promoted directly in the database, not
                // self-assigned through this endpoint. RegisterRequest has
                // no `role` field, so there is nothing for a client to set.
                .role(Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .token(token)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                // Same exception for "no such email" and "wrong password"
                // below - a distinct message here would let a caller
                // enumerate which emails are registered.
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.getActive()) {
            throw new AccountInactiveException();
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .token(token)
                .build();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}