package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.RegisterUserRequest;
import com.fakru.interview.tracker.model.request.UserLoginRequest;
import com.fakru.interview.tracker.model.request.ValidateOTPRequest;
import com.fakru.interview.tracker.service.UserService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static com.fakru.interview.tracker.constants.ApiConstants.*;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

@RestController
@Slf4j
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/user/register")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody RegisterUserRequest request,
                                                          HttpServletResponse response) {
        String token = userService.createUser(request);
        response.setHeader(AUTHORIZATION, BEARER + token);
        return new ResponseEntity<>(Map.of(MESSAGE, "user registered successfully"), HttpStatus.OK);
    }

    @PostMapping("/api/v1/user/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequest request,
                                                         HttpServletResponse response) {
        String token = userService.loginUser(request.getEmail(), request.getPassword());
        response.setHeader(AUTHORIZATION, BEARER + token);
        return new ResponseEntity<>(Map.of(MESSAGE, "login successful"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/send/emailOtp")
    public ResponseEntity<Map<String, String>> sendEmailOtp(
            @RequestParam @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9]\\d{9}$") String email,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String token = userService.sendMailOTP(claimsSet, email);
        httpResponse.setHeader(AUTHORIZATION, BEARER + token);
        return ResponseEntity.ok(Map.of(MESSAGE, "OTP sent in email"));
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/verify/emailOtp")
    public ResponseEntity<Map<String, String>> verifyEmailOtp(HttpServletRequest httpRequest,
                                                              @RequestBody ValidateOTPRequest request) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String message = userService.verifyEmailOTP(UUID.fromString(claimsSet.getSubject()), request.getOtp());
        HttpStatus status = switch (message) {
            case "Email already validated" -> HttpStatus.BAD_REQUEST;
            case "OTP expired" -> HttpStatus.UNAUTHORIZED;
            case "Invalid OTP" -> HttpStatus.FORBIDDEN;
            case "OTP verified" -> HttpStatus.OK;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return new ResponseEntity<>(Map.of(MESSAGE, message), status);
    }

    @PostMapping("/api/v1/user/initiatePasswordReset")
    public ResponseEntity<Map<String, String>> initiatePasswordReset(@RequestBody String email) {
        userService.mailPasswordResetLink(email);
        String message = "Password reset link has been sent to the provided mail id";
        return new ResponseEntity<>(Map.of(MESSAGE, message), HttpStatus.OK);
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/resetPassword")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody String newPassword,
                                                             HttpServletRequest httpRequest,
                                                             HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String token = userService.resetPassword(claimsSet.getSubject(), newPassword);
        String message = "Password has been reset successfully";
        httpResponse.setHeader(AUTHORIZATION, BEARER + token);
        return new ResponseEntity<>(Map.of(MESSAGE, message), HttpStatus.OK);
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/send/PhoneOtp")
    public ResponseEntity<Map<String, String>> sendPhoneOTP(
            @RequestParam @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9]\\d{9}$") String phoneNumber,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String token = userService.sendPhoneOTP(claimsSet, phoneNumber);
        httpResponse.setHeader(AUTHORIZATION, BEARER + token);
        return ResponseEntity.ok(Map.of(MESSAGE, "OTP sent successfully"));
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/verify/PhoneOtp")
    public ResponseEntity<Map<String, String>> verifyPhoneOTP(
            @RequestParam @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9]\\d{9}$") String phoneNumber,
            @RequestParam @Pattern(regexp = "\\d{6}") String code,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute(JWT_CLAIMS);
        String token = userService.verifyPhoneOTP(claimsSet, phoneNumber, code);
        httpResponse.setHeader(AUTHORIZATION, BEARER + token);
        return ResponseEntity.ok(Map.of(MESSAGE, "OTP verified successfully"));
    }
}
