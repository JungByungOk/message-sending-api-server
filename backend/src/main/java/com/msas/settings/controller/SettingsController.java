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

    @Operation(summary = "AWS 설정 조회", description = "현재 적용 중인 AWS 설정을 조회합니다. Secret Key는 마스킹 처리됩니다.")
    @GetMapping("/aws")
    public ResponseEntity<AwsSettingsResponseDTO> getAwsSettings() {
        return ResponseEntity.ok(settingsService.getAwsSettings());
    }

    @Operation(summary = "AWS 설정 저장", description = "AWS 연결 설정을 저장합니다. 저장 후 AWS 클라이언트가 재생성됩니다.")
    @PutMapping("/aws")
    public ResponseEntity<AwsSettingsResponseDTO> saveAwsSettings(@RequestBody AwsSettingsDTO settings) {
        return ResponseEntity.ok(settingsService.saveAwsSettings(settings));
    }

    @Operation(summary = "AWS 연결 테스트", description = "입력된 AWS 설정으로 SES/DynamoDB 연결을 테스트합니다.")
    @PostMapping("/aws/test")
    public ResponseEntity<AwsTestResultDTO> testAwsConnection(@RequestBody AwsSettingsDTO settings) {
        return ResponseEntity.ok(settingsService.testAwsConnection(settings));
    }
}
