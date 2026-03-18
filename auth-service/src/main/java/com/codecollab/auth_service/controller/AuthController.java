package com.codecollab.auth_service.controller;

/*
API Endpoints:
        • POST /api/auth/signup - Signup new user
        • POST /api/auth/login - Login with email/password
        • POST /api/auth/refresh - Refresh access token
        • POST /api/auth/logout - Logout (invalidate tokens)
        • GET /api/auth/me - Get current user profile
        • POST /api/auth/oauth/google - Login with Google
        • POST /api/auth/oauth/github - Login with GitHub
        • GET /api/workspaces - List user's workspaces
        • POST /api/workspaces - Create new workspace
        • PUT /api/workspaces/{id} - Update workspace
        • POST /api/workspaces/{id}/members - Add member

 */

import com.codecollab.auth_service.dto.login.LoginRequestDto;
import com.codecollab.auth_service.dto.login.LoginResponseDto;
import com.codecollab.auth_service.dto.signup.SignupRequestDto;
import com.codecollab.auth_service.dto.signup.SignupResponseDto;
import com.codecollab.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody SignupRequestDto requestDto){
        return ResponseEntity.ok(authService.signup(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto requestDto){
        return ResponseEntity.ok(authService.login(requestDto));
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(@RequestBody LoginRequestDto requestDto){
        authService.deleteAccount(requestDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody  LoginRequestDto requestDto){
        authService.logout(requestDto);
        return ResponseEntity.noContent().build();
    }
}
