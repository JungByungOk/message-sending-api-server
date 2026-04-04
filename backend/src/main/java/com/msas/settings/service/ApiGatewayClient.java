package com.msas.settings.service;

import com.msas.settings.entity.SystemConfigEntity;
import com.msas.settings.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * API Gateway 호출을 위한 공통 HTTP 클라이언트.
 * SYSTEM_CONFIG에서 endpoint, 인증 정보를 읽어 요청을 구성합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiGatewayClient {

    private final SystemConfigRepository systemConfigRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * API Gateway에 POST 요청을 보냅니다.
     */
    public HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest request = buildRequest("POST", path, jsonBody);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * API Gateway에 GET 요청을 보냅니다.
     */
    public HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = buildRequest("GET", path, null);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * API Gateway에 DELETE 요청을 보냅니다.
     */
    public HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = buildRequest("DELETE", path, null);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest buildRequest(String method, String path, String jsonBody) {
        String endpoint = getConfigValue("gateway.endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("API Gateway Endpoint가 설정되지 않았습니다. 설정 화면에서 먼저 구성하세요.");
        }

        String url = endpoint.replaceAll("/$", "") + path;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");

        // 인증
        String authType = getConfigValue("gateway.auth-type");
        if ("API_KEY".equals(authType)) {
            String apiKey = getConfigValue("gateway.api-key");
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("x-api-key", apiKey);
            }
        }

        // HTTP Method
        switch (method) {
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : ""));
                break;
            case "GET":
                builder.GET();
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : ""));
        }

        return builder.build();
    }

    private String getConfigValue(String key) {
        SystemConfigEntity entity = systemConfigRepository.findByKey(key);
        return entity != null ? entity.getConfigValue() : null;
    }
}
