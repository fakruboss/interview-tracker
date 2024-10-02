package com.fakru.interview.tracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR = "error";

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectPassword(IncorrectPasswordException e) {
        return new ResponseEntity<>(Map.of(ERROR, e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> userNotFound(UserNotFoundException e) {
        return new ResponseEntity<>(Map.of(ERROR, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DatabaseOperationException.class)
    public ResponseEntity<Map<String, String>> dynamoDbError(DatabaseOperationException e) {
        return new ResponseEntity<>(Map.of(ERROR, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}