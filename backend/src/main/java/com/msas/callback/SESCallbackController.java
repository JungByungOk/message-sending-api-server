package com.msas.callback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "SES Callback", description = "SES 이벤트 콜백 API")
@RestController
@RequestMapping("/ses/callback")
@RequiredArgsConstructor
public class SESCallbackController {

    private final SESCallbackService sesCallbackService;

    @Operation(summary = "SES 이벤트 처리", description = "SES에서 전송된 이메일 이벤트(전송/반송/수신거부)를 처리합니다.")
    @PostMapping("/event")
    public ResponseEntity<CallbackResponseDTO> processEvent(@RequestBody SESCallbackEventDTO event) {
        CallbackResponseDTO result = sesCallbackService.processEvent(event);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "콜백 상태 확인", description = "콜백 서비스의 상태 및 마지막 이벤트 수신 시간을 반환합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "lastEventTime", sesCallbackService.getLastEventTime() != null
                        ? sesCallbackService.getLastEventTime().toString()
                        : "없음"
        ));
    }
}
