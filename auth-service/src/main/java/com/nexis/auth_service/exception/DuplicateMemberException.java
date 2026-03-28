package com.nexis.auth_service.exception;

// Use when you try to add a member to a workspace they are already in
public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException(String message) {
        super(message);
    }
}