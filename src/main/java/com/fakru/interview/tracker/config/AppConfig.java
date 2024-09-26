package com.fakru.interview.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import service.PasswordService;

@Configuration
public class AppConfig {

    @Bean
    public PasswordService passwordService() {
        return new PasswordService(4);
    }
}