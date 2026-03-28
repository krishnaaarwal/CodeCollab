package com.nexis.auth_service.exception;

// Use  when someone tries to sign up with an email that already exists
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}