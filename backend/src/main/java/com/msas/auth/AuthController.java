package com.msas.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "로그인", description = "사용자명과 비밀번호로 JWT 토큰을 발급받습니다.")
    public ResponseEntity<AuthDTO.LoginResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급받습니다.")
    public ResponseEntity<AuthDTO.TokenResponse> refresh(@Valid @RequestBody AuthDTO.RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "로그아웃", description = "로그아웃 처리합니다. (클라이언트에서 토큰 삭제)")
    public ResponseEntity<Map<String, Boolean>> logout() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/auth/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.")
    public ResponseEntity<Map<String, Boolean>> changePassword(
            Authentication authentication,
            @Valid @RequestBody AuthDTO.ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/users/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 반환합니다.")
    public ResponseEntity<AuthDTO.UserInfo> getMe(Authentication authentication) {
        return ResponseEntity.ok(authService.getMe(authentication.getName()));
    }

    @GetMapping("/users")
    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 반환합니다.")
    public ResponseEntity<List<AuthDTO.UserInfo>> getUsers() {
        return ResponseEntity.ok(authService.getUsers());
    }

    @PostMapping("/users")
    @Operation(summary = "사용자 생성", description = "새 사용자를 생성합니다.")
    public ResponseEntity<AuthDTO.UserInfo> createUser(@Valid @RequestBody AuthDTO.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "사용자 수정", description = "사용자 정보를 수정합니다.")
    public ResponseEntity<AuthDTO.UserInfo> updateUser(
            @PathVariable int id,
            @RequestBody AuthDTO.UpdateUserRequest request) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다. admin 계정은 삭제 불가합니다.")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable int id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
