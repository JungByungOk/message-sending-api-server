package com.msas.settings.controller;

import com.msas.settings.dto.AwsSettingsDTO;
import com.msas.settings.dto.AwsSettingsResponseDTO;
import com.msas.settings.dto.AwsTestResultDTO;
import com.msas.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settings", description = "시스템 설정 관리 API")
@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "설정 조회", description = "API Gateway, Callback, 수신 모드 등 전체 설정을 조회합니다.")
    @GetMapping("/aws")
    public ResponseEntity<AwsSettingsResponseDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @Operation(summary = "설정 저장", description = "API Gateway, Callback, 수신 모드 등 전체 설정을 저장합니다.")
    @PutMapping("/aws")
    public ResponseEntity<AwsSettingsResponseDTO> saveSettings(@RequestBody AwsSettingsDTO settings) {
        return ResponseEntity.ok(settingsService.saveSettings(settings));
    }

    @Operation(summary = "API Gateway 연결 테스트", description = "입력된 설정으로 API Gateway 연결을 테스트합니다.")
    @PostMapping("/aws/test")
    public ResponseEntity<AwsTestResultDTO> testConnection(@RequestBody AwsSettingsDTO settings) {
        return ResponseEntity.ok(settingsService.testConnection(settings));
    }
}
