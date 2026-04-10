package com.msas.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtProvider jwtProvider, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        log.info("[AuthService] login attempt for username={}", request.getUsername());
        UserEntity user = userRepository.findByUsername(request.getUsername());
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        userRepository.updateLastLogin(user.getUsername());
        String accessToken = jwtProvider.createAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getUsername());
        log.info("[AuthService] login success for username={}", user.getUsername());
        return new AuthDTO.LoginResponse(accessToken, refreshToken, toUserInfo(user));
    }

    public AuthDTO.TokenResponse refresh(String refreshToken) {
        log.info("[AuthService] token refresh requested");
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new IllegalArgumentException("리프레시 토큰이 아닙니다.");
        }
        String username = jwtProvider.getUsername(refreshToken);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        String newAccessToken = jwtProvider.createAccessToken(username, user.getRole());
        log.info("[AuthService] token refreshed for username={}", username);
        return new AuthDTO.TokenResponse(newAccessToken);
    }

    public void changePassword(String username, AuthDTO.ChangePasswordRequest request) {
        log.info("[AuthService] changePassword for username={}", username);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        String newHash = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePassword(user.getUserId(), newHash);
        log.info("[AuthService] password changed for username={}", username);
    }

    public AuthDTO.UserInfo getMe(String username) {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return toUserInfo(user);
    }

    public List<AuthDTO.UserInfo> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserInfo)
                .collect(Collectors.toList());
    }

    public AuthDTO.UserInfo createUser(AuthDTO.CreateUserRequest request) {
        log.info("[AuthService] createUser username={}", request.getUsername());
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole() != null ? request.getRole() : "ADMIN");
        user.setActive(true);
        userRepository.insertUser(user);
        UserEntity created = userRepository.findByUsername(request.getUsername());
        log.info("[AuthService] user created userId={}", created.getUserId());
        return toUserInfo(created);
    }

    public AuthDTO.UserInfo updateUser(int userId, AuthDTO.UpdateUserRequest request) {
        log.info("[AuthService] updateUser userId={}", userId);
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }
        userRepository.updateUser(user);
        return toUserInfo(userRepository.findById(userId));
    }

    public void deleteUser(int userId) {
        log.info("[AuthService] deleteUser userId={}", userId);
        UserEntity user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        if ("admin".equals(user.getUsername())) {
            throw new IllegalArgumentException("admin 계정은 삭제할 수 없습니다.");
        }
        userRepository.deleteUser(userId);
        log.info("[AuthService] user deleted userId={}", userId);
    }

    private AuthDTO.UserInfo toUserInfo(UserEntity user) {
        return new AuthDTO.UserInfo(user.getUserId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
