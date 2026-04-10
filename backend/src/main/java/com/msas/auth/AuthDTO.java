package com.msas.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTO {

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private UserInfo user;

        public LoginResponse(String accessToken, String refreshToken, UserInfo user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public UserInfo getUser() { return user; }
    }

    public static class UserInfo {
        private int userId;
        private String username;
        private String displayName;
        private String role;

        public UserInfo(int userId, String username, String displayName, String role) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
        }

        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getRole() { return role; }
    }

    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class TokenResponse {
        private String accessToken;

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getAccessToken() { return accessToken; }
    }

    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank
        @Size(min = 4)
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class CreateUserRequest {
        @NotBlank
        private String username;
        @NotBlank
        @Size(min = 4)
        private String password;
        private String displayName;
        private String role = "ADMIN";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class UpdateUserRequest {
        private String displayName;
        private String role;
        private Boolean isActive;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    }
}
