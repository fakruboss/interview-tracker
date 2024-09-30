package com.fakru.interview.tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
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
        String otp = generateOTP();
        emailService.sendEmail(toEmail,
                "Use this OTP to validate email : " + otp + ". This OTP expires in 10 minutes",
                "Interview Tracker - Verify your email"
        );
        return otp;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String url = "http://localhost:8080/passwordReset?token=" + token;
        emailService.sendEmail(toEmail,
                "Click the below link to reset the password. This OTP expires in 10 minutes. Link : " + url,
                "Interview Tracker - Password reset"
        );
    }
}
