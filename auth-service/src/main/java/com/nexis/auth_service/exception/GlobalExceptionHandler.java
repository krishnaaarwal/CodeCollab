package com.nexis.auth_service.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex){
        log.warn("Bad Request: {}", ex.getMessage());
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException (UsernameNotFoundException ex){
        log.warn("User not found: {}", ex.getMessage());
        ApiError apiError = new ApiError("User not found: " + ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class, RefreshTokenNotFoundException.class, RefreshTokenExpiredException.class})
    public ResponseEntity<ApiError> handleSecurityExceptions(RuntimeException ex){
        log.warn("Security/Auth failure: {}", ex.getMessage());
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex){
        log.warn("Access denied: {}", ex.getMessage());
        ApiError apiError = new ApiError("Access denied: Insufficient permissions", HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex){
        log.warn("Not Found: {}", ex.getMessage());
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler({UserAlreadyExistsException.class, DuplicateMemberException.class})
    public ResponseEntity<ApiError> handleConflictExceptions(RuntimeException ex){
        log.warn("Data Conflict: {}", ex.getMessage());
        ApiError apiError = new ApiError(ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Grab the default message from the very first validation error
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation failed: {}", errorMessage);
        ApiError apiError = new ApiError("Invalid input: " + errorMessage, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex){
        log.error("CRITICAL: An unexpected server error occurred: ", ex);
        ApiError apiError = new ApiError("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(apiError, apiError.getHttpStatus());
    }
}