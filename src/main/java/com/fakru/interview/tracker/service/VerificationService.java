package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.config.TwilioConfig;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@Slf4j
public class VerificationService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private final EmailService emailService;
    private final TwilioConfig twilioConfig;

    @Autowired
    public VerificationService(EmailService emailService, TwilioConfig twilioConfig) {
        this.emailService = emailService;
        this.twilioConfig = twilioConfig;
    }

    public static String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    public String sendEmailOTP(String toEmail) {
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

    public boolean sendPhoneOTP(String phoneNumber) {
        try {
            Verification verification = Verification
                    .creator(twilioConfig.getServiceSid(), phoneNumber, "sms")
                    .create();

            log.info("Verification initiated for {}, Status: {}", phoneNumber, verification.getStatus());
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return false;
        }
    }

    public boolean verifyPhoneOTP(String phoneNumber, String code) {
        try {
            VerificationCheck verificationCheck = VerificationCheck
                    .creator(twilioConfig.getServiceSid())
                    .setTo(phoneNumber)
                    .setCode(code)
                    .create();

            return "approved".equals(verificationCheck.getStatus());
        } catch (Exception e) {
            log.error("Failed to verify OTP", e);
            return false;
        }
    }
}
