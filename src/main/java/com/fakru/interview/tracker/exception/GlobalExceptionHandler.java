package com.fakru.interview.tracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectPassword(IncorrectPasswordException e) {
        return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.UNAUTHORIZED);
    }
}