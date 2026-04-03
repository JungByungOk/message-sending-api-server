package com.msas.common.httplog;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request 와 Response 를 로깅하기 위한 필터
 */
@Component
public class RequestResponseWrapperFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappingRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappingResponse = new ContentCachingResponseWrapper(response);

        chain.doFilter(wrappingRequest, wrappingResponse);

        wrappingResponse.copyBodyToResponse();
    }
}
