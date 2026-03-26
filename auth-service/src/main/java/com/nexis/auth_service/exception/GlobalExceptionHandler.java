package com.nexis.auth_service.exception;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException (UsernameNotFoundException usernameNotFoundException){
        ApiError apiError = new ApiError("User not found with email : "+usernameNotFoundException.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException authenticationException){
        ApiError apiError = new ApiError("Authentication failed :"+authenticationException.getMessage(),HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException jwtException){
        ApiError apiError = new ApiError("Invalid Jwt token: "+jwtException.getMessage(),HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
    }
//
//    @ExceptionHandler(RefreshTokenNotFoundException.class)
//    public ResponseEntity<ApiError> handleRefreshTokenNotFoundException(RefreshTokenNotFoundException exception){
//        ApiError apiError = new ApiError("Refresh Token not found: "+exception.getMessage(),HttpStatus.UNAUTHORIZED);
//        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
//    }
//
//    @ExceptionHandler(RefreshTokenExpiredException.class)
//    public ResponseEntity<ApiError> handleRefreshTokenExpiredException(RefreshTokenExpiredException exception){
//        ApiError apiError = new ApiError("Refresh Token expired: "+exception.getMessage(),HttpStatus.UNAUTHORIZED);
//        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
//    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException accessDeniedException){
        ApiError apiError = new ApiError("Access denied:Insufficient permissions "+accessDeniedException.getMessage(),HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception exception){
        ApiError apiError = new ApiError("An unexpected error occurred :"+exception.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(apiError,apiError.getHttpStatus());
    }
}
