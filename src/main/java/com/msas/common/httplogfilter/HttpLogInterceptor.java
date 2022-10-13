package com.msas.common.httplogfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpLogInterceptor extends HandlerInterceptorAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

//        log.info("URI: [{}], METHOD: [{}]", request.getRequestURI(), request.getMethod());
//        log.info("Headers: {}", getHeaders(request));
//        log.info("QueryString: {}", getQueryParameter(request));
//        log.info("Request Body: {}", contentBody(cachingResponse.getContentAsByteArray()));
//        log.info("Request Body: {}", contentBody(cachingRequest.getContentAsByteArray()));

        HttpLogDTO httpLogDTO = new HttpLogDTO();
        {
            httpLogDTO.setUri(request.getRequestURI());
            httpLogDTO.setMethod(request.getMethod());
            httpLogDTO.setHeaders(getHeaders(request));
            httpLogDTO.setQueryParameters(getQueryParameter(request));
            httpLogDTO.setRequest_body(contentBody(cachingRequest.getContentAsByteArray()));
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