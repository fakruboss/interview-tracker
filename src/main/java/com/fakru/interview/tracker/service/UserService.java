package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import com.fakru.interview.tracker.dynamodata.User;
import com.fakru.interview.tracker.exception.IncorrectPasswordException;
import com.fakru.interview.tracker.exception.UserCreationFailedException;
import com.fakru.interview.tracker.model.request.RegisterUserRequest;
import com.fakru.interview.tracker.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import org.springframework.stereotype.Service;
import service.PasswordService;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import util.JoseJwtUtil;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final PasswordService passwordService;
    private final VerificationService verificationService;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, PasswordService passwordService, VerificationService verificationService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.verificationService = verificationService;
    }

    @LogExecutionTime
    public String createUser(RegisterUserRequest registerUserRequest) {
        try {
            long currentTimeMillis = System.currentTimeMillis();
            Timestamp currentTs = new Timestamp(currentTimeMillis);
            String otp = verificationService.sendVerifyEmailMessage(registerUserRequest.getEmail());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(currentTimeMillis);
            calendar.add(Calendar.HOUR, 24);

            User user = User.builder()
                    .name(registerUserRequest.getName())
                    .email(registerUserRequest.getEmail())
                    .phoneNumber(registerUserRequest.getPhoneNumber())
                    .passwordHash(passwordService.hashPassword(registerUserRequest.getPassword()))
                    .isEmailValidated(false)
                    .otpHash(passwordService.hashPassword(otp))
                    .otpExpiryTime(new Timestamp(calendar.getTimeInMillis()))
                    .createdAt(currentTs)
                    .updatedAt(currentTs)
                    .build();
            String pk = UUID.randomUUID().toString();
            userRepository.saveUser(pk, user);
            return JoseJwtUtil.generateToken(pk, new HashMap<>());
        } catch (Exception e) {
            throw new UserCreationFailedException(e.getMessage());
        }
    }

    public String loginUser(String email, String password) throws JOSEException {
        Map<String, AttributeValue> items = userRepository.findByEmail(email);
        String passwordHash = items.get("password_hash").s();
        boolean doesPasswordsMatch = passwordService.verifyPassword(password, passwordHash);
        if (doesPasswordsMatch) {
            return JoseJwtUtil.generateToken(items.get("pk").s());
        } else {
            throw new IncorrectPasswordException("Incorrect password");
        }
    }

    public String verifyOTP(UUID userId, long otp) {
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

        userRepository.updateUser(userId.toString(), updatedItem);
        return "OTP verified";
    }
}