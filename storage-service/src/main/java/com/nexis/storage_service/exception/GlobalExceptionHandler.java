package com.nexis.storage_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Security & Access Denied Boundaries
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurityException(SecurityException ex, WebRequest request) {
        log.warn("Security boundary violation detected: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden Access", ex.getMessage(), request);
    }

    // 2. Handle State or Validation Inconsistencies
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleBadRequestExceptions(RuntimeException ex, WebRequest request) {
        log.warn("Business rule or validation failure: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // 3. Handle Concurrency Race Conditions (The Unique Constraint Trigger!)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Database constraint broken or race condition intercepted", ex);

        String userFriendlyMessage = "A file with this name already exists in this workspace.";
        return buildResponse(HttpStatus.CONFLICT, "Data Conflict", userFriendlyMessage, request);
    }

    // 4. Global Catch-All Fallback (Prevents raw system leakage)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaughtExceptions(Exception ex, WebRequest request) {
        log.error("Critical uncaught system level failure encountered", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.", request);
    }

    @ExceptionHandler(UploadIntentNotFoundException.class)
    public ResponseEntity<ApiError> handleUploadIntentNotFoundExceptions(UploadIntentNotFoundException ex, WebRequest request) {
        log.error("Upload file not found", ex);
        return buildResponse(HttpStatus.valueOf(404), "Upload Intent Not Found", "Upload file not found", request);
    }

    @ExceptionHandler(FileVersionConflictException.class)
    public ResponseEntity<ApiError> handleFileVersionConflictExceptions(FileVersionConflictException ex, WebRequest request) {
        log.error("Version Conflict", ex);
        return buildResponse(HttpStatus.valueOf(409), "Version Conflict", "There is version conflict in files", request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String error, String message, WebRequest request) {
        ApiError response = new ApiError(
                status.value(),
                error,
                message,
                request.getDescription(false).replace("uri=", ""),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, status);
    }
}
