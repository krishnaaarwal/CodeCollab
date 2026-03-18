package com.codecollab.auth_service.service;

import com.codecollab.auth_service.dto.login.LoginRequestDto;
import com.codecollab.auth_service.dto.login.LoginResponseDto;
import com.codecollab.auth_service.dto.signup.SignupRequestDto;
import com.codecollab.auth_service.dto.signup.SignupResponseDto;

public class AuthServiceImplementation implements AuthService{

    @Override
    public SignupResponseDto signup(SignupRequestDto requestDto) {
        return null;
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {
        return null;
    }

    @Override
    public void deleteAccount(LoginRequestDto requestDto) {

    }

    @Override
    public void logout(LoginRequestDto requestDto) {

    }
}
