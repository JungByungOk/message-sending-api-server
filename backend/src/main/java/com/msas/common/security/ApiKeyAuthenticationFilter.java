package com.msas.common.security;

import com.msas.common.tenant.TenantContext;
import com.msas.tenant.entity.TenantEntity;
import com.msas.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_TENANT_ID = "default";

    private final String legacyApiKey;
    private final TenantRepository tenantRepository;

    public ApiKeyAuthenticationFilter(String legacyApiKey, TenantRepository tenantRepository) {
        this.legacyApiKey = legacyApiKey;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // JWT 필터에서 이미 인증된 경우 건너뜀
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String requestApiKey = null;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            // JWT 형식(점 2개)이면 건너뜀
            if (token.chars().filter(c -> c == '.').count() == 2) {
                filterChain.doFilter(request, response);
                return;
            }
            requestApiKey = token;
        } else if (authHeader != null) {
            requestApiKey = authHeader.trim();
        }

        if (requestApiKey == null || requestApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 멀티 테넌트 DB 조회
        TenantEntity tenant = tenantRepository.selectTenantByApiKey(requestApiKey);
        if (tenant != null) {
            TenantContext.setTenantId(tenant.getTenantId());
            authenticate(response, filterChain, request, tenant.getTenantId());
            return;
        }

        // 2. 레거시 단일 API 키 매칭
        if (legacyApiKey != null && legacyApiKey.equals(requestApiKey)) {
            TenantContext.setTenantId(DEFAULT_TENANT_ID);
            authenticate(response, filterChain, request, DEFAULT_TENANT_ID);
            return;
        }

        // 3. 인증 실패
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 API 키입니다.");
    }

    private void authenticate(HttpServletResponse response, FilterChain filterChain,
                               HttpServletRequest request, String principal)
            throws ServletException, IOException {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_API")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
