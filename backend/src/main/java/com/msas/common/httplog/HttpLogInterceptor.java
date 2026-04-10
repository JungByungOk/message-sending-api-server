package com.msas.common.httplog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * HTTP Request 와 Response 로깅 인터셉터
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class HttpLogInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /** 로그에서 제외할 헤더 (소문자 기준) */
    private static final Set<String> EXCLUDED_HEADERS = Set.of(
            "user-agent", "accept-encoding", "accept-language",
            "cache-control", "connection", "sec-fetch-dest",
            "sec-fetch-mode", "sec-fetch-site", "sec-ch-ua",
            "sec-ch-ua-mobile", "sec-ch-ua-platform", "upgrade-insecure-requests"
    );

    /** 마스킹 대상 헤더 (소문자 기준) */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "x-api-key", "x-callback-secret"
    );

    /** 요청/응답 본문 최대 로깅 길이 */
    private static final int MAX_BODY_LENGTH = 2000;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex)
            throws Exception {

        final ContentCachingRequestWrapper cachingRequest;
        ContentCachingRequestWrapper cachingRequest1;
        final ContentCachingResponseWrapper cachingResponse;
        ContentCachingResponseWrapper cachingResponse1;

        try {
            cachingRequest1 = (ContentCachingRequestWrapper) request;
            cachingResponse1 = (ContentCachingResponseWrapper) response;
        } catch (ClassCastException e) {
            cachingRequest1 = null;
            cachingResponse1 = null;
        }

        cachingResponse = cachingResponse1;
        cachingRequest = cachingRequest1;

        HttpLogDTO httpLogDTO = new HttpLogDTO();
        {
            httpLogDTO.setRemoteInfo(
                    new HttpLogRemoteInfoDTO.HttpLogRemoteInfoDTOBuilder().RemoteHost(request.getRemoteHost())
                            .RemoteIp(request.getRemoteAddr())
                            .RemotePort(request.getRemotePort())
                            .build());

            httpLogDTO.setUri(request.getRequestURI());
            httpLogDTO.setMethod(request.getMethod());
            httpLogDTO.setHeaders(getHeaders(request));
            httpLogDTO.setQueryParameters(getQueryParameter(request));

            if (cachingRequest != null)
                httpLogDTO.setRequest_body(truncateBody(contentBody(cachingRequest.getContentAsByteArray())));

            if (cachingResponse != null)
                httpLogDTO.setResponse_body(truncateBody(contentBody(cachingResponse.getContentAsByteArray())));
        }
        log.info("[HttpLog] {} {} (status={})\n{}", httpLogDTO.getMethod(), httpLogDTO.getUri(),
                response.getStatus(), new GsonBuilder().setPrettyPrinting().create().toJson(httpLogDTO));
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>();

        Enumeration<String> headerArray = request.getHeaderNames();
        while (headerArray.hasMoreElements()) {
            String headerName = headerArray.nextElement();
            String lowerName = headerName.toLowerCase();

            if (EXCLUDED_HEADERS.contains(lowerName)) {
                continue;
            }

            if (SENSITIVE_HEADERS.contains(lowerName)) {
                headerMap.put(headerName, maskValue(request.getHeader(headerName)));
            } else {
                headerMap.put(headerName, request.getHeader(headerName));
            }
        }
        return headerMap;
    }

    private Map<String, String> getQueryParameter(HttpServletRequest request) {
        Map<String, String> queryMap = new HashMap<>();
        request.getParameterMap()
                .forEach((key, value) -> queryMap.put(key, String.join("", value)));
        return queryMap;
    }

    private String contentBody(final byte[] contents) {
        StringBuilder sb = new StringBuilder("\n");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), java.nio.charset.StandardCharsets.UTF_8));
        bufferedReader.lines().forEach(str -> sb.append(str).append("\n"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String truncateBody(String body) {
        if (body == null) return null;
        if (body.length() <= MAX_BODY_LENGTH) return body;
        return body.substring(0, MAX_BODY_LENGTH) + "... (truncated, total=" + body.length() + ")";
    }

    private String maskValue(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

}