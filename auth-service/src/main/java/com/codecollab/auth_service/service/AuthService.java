package com.codecollab.auth_service.service;

import com.codecollab.auth_service.dto.login.LoginRequestDto;
import com.codecollab.auth_service.dto.login.LoginResponseDto;
import com.codecollab.auth_service.dto.signup.SignupRequestDto;
import com.codecollab.auth_service.dto.signup.SignupResponseDto;

public interface AuthService {
    SignupResponseDto signup(SignupRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto);

    void deleteAccount(LoginRequestDto requestDto);

    void logout(LoginRequestDto requestDto);
}
