package com.example.auth_service.dto;

import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * What GET /auth/user/{userId} returns. Deliberately excludes the password
 * hash - the entity has that field, this DTO does not.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean active;
    private Role role;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.getActive())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}