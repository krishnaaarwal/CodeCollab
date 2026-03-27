package com.nexis.auth_service.service;


import com.nexis.auth_service.dto.login.LoginRequestDto;
import com.nexis.auth_service.dto.login.LoginResponseDto;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.SignupRequestDto;
import com.nexis.auth_service.dto.signup.SignupResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    SignupResponseDto signup(SignupRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto);

    ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId);


    void logout(LogoutRequestDto requestDto);

    LoginResponseDto refreshToken(RefreshTokenRequestDto body);
}
