package com.msas.common.httplog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Request 와 Response 로깅 인터셉터
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class HttpLogInterceptor extends HandlerInterceptorAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        final ContentCachingRequestWrapper cachingRequest;
        ContentCachingRequestWrapper cachingRequest1;

        try {
            cachingRequest1 = (ContentCachingRequestWrapper) request;
        } catch (ClassCastException e) {
            cachingRequest1 = null;
        }

        cachingRequest = cachingRequest1;
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
                httpLogDTO.setRequest_body(contentBody(cachingRequest.getContentAsByteArray()));

            if (cachingResponse != null)
                httpLogDTO.setResponse_body(contentBody(cachingResponse.getContentAsByteArray()));
        }
        log.info("{} {}\n{}\n", httpLogDTO.getMethod(), httpLogDTO.getUri(), new GsonBuilder().setPrettyPrinting().create().toJson(httpLogDTO).toString());
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headerMap = new HashMap<>();

        Enumeration<String> headerArray = request.getHeaderNames();
        while (headerArray.hasMoreElements()) {
            String headerName = headerArray.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
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
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents)));
        bufferedReader.lines().forEach(str -> sb.append(str).append("\n"));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}