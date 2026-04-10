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

    @Operation(summary = "VDM 상태 조회", description = "Virtual Deliverability Manager 활성화 상태를 조회합니다.")
    @GetMapping("/vdm")
    public ResponseEntity<java.util.Map<String, Object>> getVdmStatus() {
        try {
            return ResponseEntity.ok(settingsService.getVdmStatus());
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("enabled", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "VDM ON/OFF", description = "Virtual Deliverability Manager를 활성화/비활성화합니다.")
    @PutMapping("/vdm")
    public ResponseEntity<java.util.Map<String, Object>> updateVdm(@RequestBody java.util.Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", false);
        return ResponseEntity.ok(settingsService.updateVdm(enabled));
    }

    @Operation(summary = "폴링 주기 조회", description = "현재 보정 폴링 주기를 조회합니다.")
    @GetMapping("/polling-interval")
    public ResponseEntity<java.util.Map<String, Object>> getPollingInterval() {
        long intervalMs = settingsService.getPollingInterval();
        return ResponseEntity.ok(java.util.Map.of(
                "intervalMinutes", intervalMs / 60000,
                "intervalMs", intervalMs
        ));
    }

    @Operation(summary = "폴링 주기 변경", description = "보정 폴링 주기를 변경합니다 (1~10분).")
    @PutMapping("/polling-interval")
    public ResponseEntity<java.util.Map<String, Object>> updatePollingInterval(@RequestBody java.util.Map<String, Integer> request) {
        int minutes = request.getOrDefault("intervalMinutes", 2);
        long intervalMs = settingsService.updatePollingInterval(minutes);
        return ResponseEntity.ok(java.util.Map.of(
                "intervalMinutes", minutes,
                "intervalMs", intervalMs
        ));
    }
}
