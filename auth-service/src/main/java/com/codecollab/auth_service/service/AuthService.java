package com.codecollab.auth_service.service;

import com.codecollab.auth_service.dto.login.LoginRequestDto;
import com.codecollab.auth_service.dto.login.LoginResponseDto;
import com.codecollab.auth_service.dto.signup.SignupRequestDto;
import com.codecollab.auth_service.dto.signup.SignupResponseDto;
import com.codecollab.auth_service.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    SignupResponseDto signup(SignupRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto);

    ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId);

    void deleteAccount(LoginRequestDto requestDto);

    void logout(LoginRequestDto requestDto);
}
