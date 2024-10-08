package com.fakru.interview.tracker.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties
@Getter
@Setter
@Slf4j
public class RateLimitConfig {
    private List<PathConfig> rateLimits;

    @Getter
    @Setter
    public static class PathConfig {
        private String path;
        private Map<String, MethodConfig> methods = new HashMap<>();
    }

    @Getter
    @Setter
    public static class MethodConfig {
        private int limit;
        private int duration;
    }
}