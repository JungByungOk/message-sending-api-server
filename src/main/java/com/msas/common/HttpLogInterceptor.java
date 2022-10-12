package com.msas.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpLogInterceptor extends HandlerInterceptorAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

        log.info(
                "->[REQ] : {}",
                objectMapper.readTree(cachingRequest.getContentAsByteArray())
        );

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

        log.info(
                "<-[RES] : {}",
                objectMapper.readTree(cachingResponse.getContentAsByteArray())
        );
    }

}