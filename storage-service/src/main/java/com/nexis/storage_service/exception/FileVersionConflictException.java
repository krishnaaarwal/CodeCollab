package com.nexis.storage_service.exception;

public class FileVersionConflictException extends RuntimeException {
    public FileVersionConflictException(String message) {
        super(message);
    }
}
