package com.fakru.interview.tracker.config;

import com.fakru.interview.tracker.interceptor.JwtAuthenticationInterceptor;
import com.fakru.interview.tracker.interceptor.DynamicRateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final DynamicRateLimitingInterceptor dynamicRateLimitingInterceptor;

    public WebConfig(JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
                     DynamicRateLimitingInterceptor dynamicRateLimitingInterceptor) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.dynamicRateLimitingInterceptor = dynamicRateLimitingInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor).order(1);
        registry.addInterceptor(dynamicRateLimitingInterceptor).order(2);
    }
}
