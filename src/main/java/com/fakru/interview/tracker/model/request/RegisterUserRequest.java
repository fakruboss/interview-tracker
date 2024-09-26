package com.fakru.interview.tracker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserRequest {
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
