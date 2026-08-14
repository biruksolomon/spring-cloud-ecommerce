package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.GoogleAuthRequest;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.RefreshResponse;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.AccountInactiveException;
import com.example.auth_service.exception.EmailAlreadyRegisteredException;
import com.example.auth_service.exception.InvalidCredentialsException;
import com.example.auth_service.exception.InvalidGoogleTokenException;
import com.example.auth_service.exception.InvalidRefreshTokenException;
import com.example.auth_service.exception.UserNotFoundException;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.GoogleTokenVerifier;
import com.example.auth_service.security.JwtProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider, RefreshTokenService refreshTokenService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.googleTokenVerifier = googleTokenVerifier;
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
                .provider(AuthProvider.LOCAL)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return issueAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                // Same exception for "no such email" and "wrong password"
                // below - a distinct message here would let a caller
                // enumerate which emails are registered.
                .orElseThrow(InvalidCredentialsException::new);

        // A Google-only account (see createGoogleUser) has no password
        // hash at all. Checking for null first avoids handing null into
        // the encoder, which throws IllegalArgumentException instead of
        // failing the login cleanly - and it means "this email exists but
        // has no password" gives the exact same response as a wrong
        // password, so it's not enumerable either.
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.getActive()) {
            throw new AccountInactiveException();
        }

        return issueAuthResponse(user);
    }

    /**
     * Exchanges a valid, unexpired, unrevoked refresh token for a new
     * access token and a new refresh token (rotation - the presented
     * token is revoked as part of this call, see RefreshTokenService).
     */
    public RefreshResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!user.getActive()) {
            throw new AccountInactiveException();
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return RefreshResponse.builder()
                .token(accessToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationMs() / 1000)
                .refreshToken(rotation.newRawToken())
                .build();
    }

    /**
     * Exchanges a Google ID token (obtained by the frontend via Google's own
     * sign-in SDK - this service never sees the user's Google password or
     * handles a redirect) for our own access/refresh token pair, the same
     * pair issueAuthResponse() hands out for register()/login().
     * <p>
     * First sign-in with a given Google account creates a new CUSTOMER user
     * with no local password. If the Google account's email already
     * belongs to an existing (password-based) user, the two are linked by
     * googleId instead of creating a duplicate account - so a user who
     * registered normally can also sign in with Google afterwards, and
     * either way ends up as the same account.
     */
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        // Google can issue tokens for unverified emails (e.g. mid sign-up
        // flows on their side); refusing those here keeps "verified
        // Google email" as strong a guarantee as our own email field.
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException();
        }

        String email = payload.getEmail();
        String googleId = payload.getSubject();

        User user = userRepository.findByEmail(email)
                .map(existing -> linkGoogleAccount(existing, googleId))
                .orElseGet(() -> createGoogleUser(payload, email, googleId));

        if (!user.getActive()) {
            throw new AccountInactiveException();
        }

        return issueAuthResponse(user);
    }

    private User linkGoogleAccount(User existing, String googleId) {
        if (existing.getGoogleId() == null) {
            existing.setGoogleId(googleId);
            userRepository.save(existing);
        }
        return existing;
    }

    private User createGoogleUser(GoogleIdToken.Payload payload, String email, String googleId) {
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");

        User user = User.builder()
                .email(email)
                // No password on purpose - this account can only ever sign
                // in via Google. login() already guards against a null
                // password here being passed into the encoder.
                .password(null)
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName != null ? lastName : "")
                .active(true)
                .role(Role.CUSTOMER)
                .provider(AuthProvider.GOOGLE)
                .googleId(googleId)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        return user;
    }

    /** Revokes a single refresh token - the device/session that presented it can no longer refresh. */
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /** Revokes every refresh token for a user - signs the user out everywhere. */
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.issue(user.getId());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .token(accessToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationMs() / 1000)
                .refreshToken(refreshToken)
                .build();
    }
}