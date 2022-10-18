package com.msas.common.httplog;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final HttpLogInterceptor httpLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpLogInterceptor)
                .excludePathPatterns("/ses/feedback/*") //AWS SNS or Webhook 호출은 제외 한다.
                .addPathPatterns("/**");
    }
}