package com.fakru.interview.tracker.controller;

import com.fakru.interview.tracker.annotation.JwtAuthenticate;
import com.fakru.interview.tracker.model.request.RegisterUserRequest;
import com.fakru.interview.tracker.model.request.UserLoginRequest;
import com.fakru.interview.tracker.model.request.ValidateOTPRequest;
import com.fakru.interview.tracker.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/user/register")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody RegisterUserRequest registerUserRequest,
                                                          HttpServletResponse response) {
        String token = userService.createUser(registerUserRequest);
        response.setHeader("Authorization", "Bearer " + token);
        return new ResponseEntity<>(Map.of("message", "user registered successfully"), HttpStatus.OK);
    }

    @PostMapping("/api/v1/user/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequest userLoginRequest,
                                                         HttpServletResponse response)
            throws JOSEException {
        String token = userService.loginUser(userLoginRequest.getEmail(), userLoginRequest.getPassword());
        response.setHeader("Authorization", "Bearer " + token);
        return new ResponseEntity<>(Map.of("message", "login successful"), HttpStatus.OK);
    }

    @JwtAuthenticate
    @PostMapping("/api/v1/user/verifyEmail")
    public ResponseEntity<Map<String, String>> verifyEmail(HttpServletRequest httpRequest,
                                                           @RequestBody ValidateOTPRequest request)
            throws JsonProcessingException {
        JWTClaimsSet claimsSet = (JWTClaimsSet) httpRequest.getAttribute("jwtClaims");
        String message = userService.verifyOTP(UUID.fromString(claimsSet.getSubject()), request.getOtp());
        HttpStatus status = switch (message) {
            case "Email already validated" -> HttpStatus.BAD_REQUEST;
            case "OTP expired" -> HttpStatus.UNAUTHORIZED;
            case "Invalid OTP" -> HttpStatus.FORBIDDEN;
            case "OTP verified" -> HttpStatus.OK;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return new ResponseEntity<>(Map.of("message", message), status);
    }
}
