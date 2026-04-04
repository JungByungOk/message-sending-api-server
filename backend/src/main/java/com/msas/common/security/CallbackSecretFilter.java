package com.msas.common.security;

import com.msas.settings.service.SettingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * /ses/callback/** 요청 시 X-Callback-Secret 헤더를 검증하는 필터.
 * Secret이 설정되지 않은 경우 검증을 건너뜁니다.
 */
@Slf4j
@RequiredArgsConstructor
public class CallbackSecretFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Callback-Secret";

    private final SettingsService settingsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String secret = settingsService.getCallbackSecret();

        // Secret 미설정 시 검증 건너뜀
        if (secret == null || secret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerValue = request.getHeader(HEADER_NAME);
        if (secret.equals(headerValue)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("CallbackSecretFilter - 유효하지 않은 Callback Secret. (remoteAddr: {})", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid Callback Secret\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/ses/callback");
    }
}
