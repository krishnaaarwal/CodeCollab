package com.nexis.auth_service.exception;

// Use when a Workspace or User UUID cannot be found in the database
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}