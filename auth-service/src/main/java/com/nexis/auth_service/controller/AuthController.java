package com.nexis.auth_service.controller;

/*
API Endpoints:
        • POST /api/auth/register - Signup new user
        • POST /api/auth/login - Login with email/password
        • POST /api/auth/refresh - Refresh access token
        • POST /api/auth/logout - Logout (invalidate tokens)
        • GET /api/auth/me - Get current user profile
        • POST /api/auth/oauth/google - Login with Google -LATER
        • POST /api/auth/oauth/github - Login with GitHub -LATER

 */


import com.nexis.auth_service.dto.login.*;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.*;
import com.nexis.auth_service.dto.user_profile.UserProfileResponseDto;
import com.nexis.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody SignupRequestDto requestDto){
        log.info("Received signup request for email: {}", requestDto.getEmail());
        return ResponseEntity.ok(authService.signup(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto requestDto){
        log.info("Received login request for email: {}", requestDto.getEmail());
        return ResponseEntity.ok(authService.login(requestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(@RequestBody RefreshTokenRequestDto body){
        log.info("Received token refresh request");
        return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(body));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequestDto requestDto){
        log.info("Received logout request");
        authService.logout(requestDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getCurrentUserProfile(){
        log.info("Fetching current user profile data");
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }
}