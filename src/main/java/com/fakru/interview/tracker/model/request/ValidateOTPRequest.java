package com.fakru.interview.tracker.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateOTPRequest {

    @NotNull(message = "OTP cannot be null")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 characters long")
    private int otp;
}
