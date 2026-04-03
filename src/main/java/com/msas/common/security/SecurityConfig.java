package com.msas.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.api-key:#{null}}")
    private String apiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Actuator health/info는 인증 없이 허용
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // SES Feedback (AWS SNS 콜백)은 인증 없이 허용
                        .requestMatchers("/ses/feedback/**").permitAll()
                        // API Key가 설정되지 않은 경우 전체 허용 (하위 호환)
                        .anyRequest().authenticated()
                );

        if (apiKey != null && !apiKey.isBlank()) {
            http.addFilterBefore(new ApiKeyAuthenticationFilter(apiKey),
                    UsernamePasswordAuthenticationFilter.class);
        } else {
            // API Key 미설정 시 전체 허용
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }
}
