package com.nexis.auth_service.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiError {
    private LocalDateTime errorTime;
    private String message;

    private HttpStatus httpStatus;

    public ApiError(){
        this.errorTime = LocalDateTime.now();
    }

    public ApiError(String message,HttpStatus httpStatus){
        this();
        this.message=message;
        this.httpStatus=httpStatus;
    }
}