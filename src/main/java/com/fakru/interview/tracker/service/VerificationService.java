package com.fakru.interview.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
public class VerificationService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private final EmailService emailService;

    @Autowired
    public VerificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public static String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    public String sendVerifyEmailMessage(String toEmail) {
        log.info("sendVerifyEmailMessage in thread: {}", Thread.currentThread().getName());
        String otp = generateOTP();
        emailService.sendEmail(toEmail,
                "Use this OTP to validate email : " + otp + ". This OTP expires in 10 minutes",
                "Interview Tracker - Verify your email"
        );
        return otp;
    }
}
