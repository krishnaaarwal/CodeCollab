package com.nexis.recording_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleDomainValidation(IllegalArgumentException ex, WebRequest request) {
        log.error("Application validation check failed", ex);

        String dynamicErrorMessage = ex.getMessage();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failure",
                dynamicErrorMessage,
                request
        );
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
