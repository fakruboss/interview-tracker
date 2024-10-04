package com.fakru.interview.tracker.dynamodata;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Builder
@Data
public class User {
    private String name;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private boolean isEmailValidated;
    private boolean isPhoneValidated;
    private String otpHash;
    private Timestamp otpExpiryTime;
    private long tokenVersion;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}