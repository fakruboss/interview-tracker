package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import com.fakru.interview.tracker.dynamodata.User;
import com.fakru.interview.tracker.exception.IncorrectPasswordException;
import com.fakru.interview.tracker.exception.UserCreationFailedException;
import com.fakru.interview.tracker.exception.VerificationException;
import com.fakru.interview.tracker.model.request.RegisterUserRequest;
import com.fakru.interview.tracker.repository.UserRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.PasswordService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import util.JoseJwtUtil;
import util.StringUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    private final PasswordService passwordService;
    private final VerificationService verificationService;
    private final UserRepository userRepository;
    private final TokenVersionService tokenVersionService;

    public UserService(UserRepository userRepository, PasswordService passwordService,
                       VerificationService verificationService, TokenVersionService tokenVersionService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.verificationService = verificationService;
        this.tokenVersionService = tokenVersionService;
    }

    @LogExecutionTime
    public String createUser(RegisterUserRequest registerUserRequest) {
        String pk = UUID.randomUUID().toString();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            Timestamp currentTs = new Timestamp(currentTimeMillis);

            User user = User.builder()
                    .name(registerUserRequest.getName())
                    .email(registerUserRequest.getEmail())
                    .phoneNumber(registerUserRequest.getPhoneNumber())
                    .passwordHash(passwordService.hashPassword(registerUserRequest.getPassword()))
                    .isEmailValidated(false)
                    .isPhoneValidated(false)
                    .tokenVersion(1)
                    .createdAt(currentTs)
                    .updatedAt(currentTs)
                    .build();
            userRepository.saveUser(pk, user);
        } catch (Exception e) {
            throw new UserCreationFailedException(e.getMessage());
        }
        return JoseJwtUtil.generateSafeToken(pk);
    }

    @LogExecutionTime
    public String loginUser(String email, String password) {
        Map<String, AttributeValue> items = userRepository.findByEmail(email);
        String passwordHash = items.get("password_hash").s();
        boolean doesPasswordsMatch = passwordService.verifyPassword(password, passwordHash);
        if (doesPasswordsMatch) {
            return JoseJwtUtil.generateSafeToken(items.get("pk").s(),
                    Map.of("tokenVersion", Integer.parseInt(items.get("token_version").n())));
        } else {
            throw new IncorrectPasswordException("Incorrect password");
        }
    }

    @LogExecutionTime
    public String verifyEmailOTP(UUID userId, long otp) {
        Map<String, AttributeValue> items = userRepository.getUser(userId.toString());
        if (Boolean.TRUE.equals(items.get("is_email_validated").bool())) {
            return "Email already validated";
        }
        if (Long.parseLong(items.get("otp_expiry_time").n()) < System.currentTimeMillis()) {
            return "OTP expired";
        }
        if (passwordService.verifyPassword(String.valueOf(otp), items.get("otp_hash").s())) {
            return "Invalid OTP";
        }

        Map<String, AttributeValue> updatedItem = new HashMap<>();
        updatedItem.put("is_email_validated", AttributeValue.builder().bool(true).build());
        updatedItem.put("otp_hash", AttributeValue.builder().s("").build());
        updatedItem.put("otp_expiry_time", AttributeValue.builder().n(String.valueOf(0L)).build());
        updatedItem.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());

        userRepository.updateUser(userId.toString(), updatedItem, new HashMap<>());
        return "OTP verified";
    }

    @LogExecutionTime
    public void mailPasswordResetLink(String email) {
        Map<String, AttributeValue> items = userRepository.findByEmail(email);
        String userId = items.get("pk").s();
        String token = JoseJwtUtil.generateSafeTokenWithExpiryTime(userId, new HashMap<>(), 60 * 24L);
        verificationService.sendPasswordResetEmail(email, token);
    }

    @LogExecutionTime
    public String resetPassword(String userId, String password) {
        Map<String, AttributeValue> setFields = new HashMap<>();
        setFields.put("is_email_validated", AttributeValue.builder().bool(true).build());
        setFields.put("password", AttributeValue.builder().s(passwordService.hashPassword(password)).build());
        setFields.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());

        Map<String, AttributeValue> addFields = new HashMap<>();
        addFields.put("token_version", AttributeValue.builder().n("1").build());

        userRepository.updateUser(userId, setFields, addFields);
        tokenVersionService.invalidateTokenVersion(userId);

        return JoseJwtUtil.generateSafeToken(userId);
    }

    @LogExecutionTime
    public String sendMailOTP(JWTClaimsSet claimsSet, String email) {
        String otp = verificationService.sendEmailOTP(email);
        if (StringUtils.isEmpty(otp)) {
            throw new VerificationException("Error while sending OTP. Please try again after sometime");
        }

        long currentTimeMillis = System.currentTimeMillis();
        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000L;  // 24 hours in milliseconds
        long futureTimeMillis = currentTimeMillis + twentyFourHoursInMillis;

        Map<String, AttributeValue> updatedItem = new HashMap<>();
        updatedItem.put("otp_hash", AttributeValue.builder().s(passwordService.hashPassword(otp)).build());
        updatedItem.put("otp_expiry_time", AttributeValue.builder().n(String.valueOf(futureTimeMillis)).build());
        updatedItem.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        userRepository.updateUser(claimsSet.getSubject(), updatedItem, new HashMap<>());

        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
    }

    @LogExecutionTime
    public String sendPhoneOTP(JWTClaimsSet claimsSet, String phoneNumber) {
        boolean isSent = verificationService.sendPhoneOTP(phoneNumber);
        if (!isSent) {
            throw new VerificationException("Error while sending OTP. Please try again after sometime");
        }
        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
    }

    @LogExecutionTime
    public String sendPhoneOTP1(JWTClaimsSet claimsSet, String phoneNumber) {
        boolean isSent = verificationService.sendPhoneOTP(phoneNumber);
        if (!isSent) {
            throw new VerificationException("Error while sending OTP. Please try again after sometime");
        }
        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
    }

    @LogExecutionTime
    public String verifyPhoneOTP(JWTClaimsSet claimsSet, String phoneNumber, String code) {
        boolean isValid = verificationService.verifyPhoneOTP(phoneNumber, code);
        if (!isValid) {
            throw new VerificationException("Error while validating OTP / incorrect OTP");
        }
        Map<String, AttributeValue> updatedItem = new HashMap<>();
        updatedItem.put("is_phone_validated", AttributeValue.builder().bool(true).build());
        updatedItem.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        userRepository.updateUser(claimsSet.getSubject(), updatedItem, new HashMap<>());
        return JoseJwtUtil.generateSafeToken(claimsSet.getSubject(), claimsSet.getClaims());
    }
}
