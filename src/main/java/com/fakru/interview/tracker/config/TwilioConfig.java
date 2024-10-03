package com.fakru.interview.tracker.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twilio")
@Data
public class TwilioConfig {
    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.service.id}")
    private String serviceSid;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
}