package com.nexis.auth_service.service;


import com.nexis.auth_service.dto.forgot_password.ForgotPasswordRequestDto;
import com.nexis.auth_service.dto.forgot_password.ResetPasswordRequestDto;
import com.nexis.auth_service.dto.login.LoginRequestDto;
import com.nexis.auth_service.dto.login.LoginResponseDto;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.SignupRequestDto;
import com.nexis.auth_service.dto.signup.SignupResponseDto;
import com.nexis.auth_service.dto.user_profile.UserProfileResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface AuthService {
    SignupResponseDto signup(SignupRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto);

    ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId);


    void logout(LogoutRequestDto requestDto,String authHeader);

    LoginResponseDto refreshToken(RefreshTokenRequestDto body);

    UserProfileResponseDto getCurrentUserProfile();

    void forgotPassword(@Valid ForgotPasswordRequestDto requestDto);

    void resetPassword(@Valid ResetPasswordRequestDto requestDto);

    boolean verifyWorkspaceMembership(UUID workspaceId, UUID userId);
}
