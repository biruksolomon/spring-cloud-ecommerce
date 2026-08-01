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

    @Column(nullable = false)
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
}