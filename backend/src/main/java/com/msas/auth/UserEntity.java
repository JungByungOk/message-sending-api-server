package com.msas.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {
    private int userId;
    private String username;
    private String passwordHash;
    private String displayName;
    private String role;
    private boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
