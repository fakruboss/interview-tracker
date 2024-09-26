package com.fakru.interview.tracker.exception;

public class UserCreationFailedException extends RuntimeException {
    public UserCreationFailedException(String message) {
        super(message);
    }
}