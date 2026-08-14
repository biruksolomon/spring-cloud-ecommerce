package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Nullable on purpose: an account created via Google sign-in has no
    // local password at all (see AuthService.createGoogleUser). Any code
    // that checks a password against this field must handle null - see
    // AuthService.login(), which rejects with InvalidCredentialsException
    // instead of passing null into the password encoder.
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private Boolean active = true;

    // DB-level default keeps this NOT NULL migration safe against rows that
    // existed before the role column was added.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'CUSTOMER'")
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt ;

    // How this account authenticates. DB-level default keeps this NOT
    // NULL migration safe against rows that existed before this column
    // was added (same reasoning as `role` above) - every pre-existing row
    // is a password account, so LOCAL is the correct backfill value.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'LOCAL'")
    private AuthProvider provider;

    // Google's stable per-user subject ("sub") claim. Null for accounts
    // that have never signed in with Google. Unique (not nullable=false,
    // since most rows will be null) so the same Google account can never
    // back two different users.
    @Column(unique = true)
    private String googleId;
}