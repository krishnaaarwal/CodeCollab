package com.nexis.auth_service.controller;

/*
API Endpoints:
        • POST /api/auth/signup - Signup new user
        • POST /api/auth/login - Login with email/password
        • POST /api/auth/refresh - Refresh access token
        • POST /api/auth/logout - Logout (invalidate tokens)
        • GET /api/auth/me - Get current user profile
        * POST /api/auth/forgot-password - Forget password
        * POST /api/auth/reset-password - Reset password
        • POST /api/auth/oauth/google - Login with Google -LATER
        • POST /api/auth/oauth/github - Login with GitHub -LATER

 */


import com.nexis.auth_service.dto.forgot_password.ForgotPasswordRequestDto;
import com.nexis.auth_service.dto.forgot_password.ResetPasswordRequestDto;
import com.nexis.auth_service.dto.login.*;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.*;
import com.nexis.auth_service.dto.user_profile.UserProfileResponseDto;
import com.nexis.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody @Valid SignupRequestDto requestDto){
        log.info("Received signup request for email: {}", requestDto.getEmail());
        return ResponseEntity.ok(authService.signup(requestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto){
        log.info("Received login request for email: {}", requestDto.getEmail());
        return ResponseEntity.ok(authService.login(requestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(@RequestBody @Valid RefreshTokenRequestDto body){
        log.info("Received token refresh request");
        return ResponseEntity.status(HttpStatus.OK).body(authService.refreshToken(body));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequestDto requestDto,@RequestHeader("Authorization") String authHeader){
        log.info("Received logout request");
        authService.logout(requestDto,authHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDto> getCurrentUserProfile(){
        log.info("Fetching current user profile data");
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDto requestDto) {
        log.info("Received forgot password request for: {}", requestDto.getEmail());
        authService.forgotPassword(requestDto);
        return ResponseEntity.ok("If an account exists, a reset code has been sent to the email.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequestDto requestDto) {
        log.info("Received reset password request for: {}", requestDto.getEmail());
        authService.resetPassword(requestDto);
        return ResponseEntity.ok("Password successfully reset.");
    }

    @GetMapping("/internal/workspaces/{workspaceId}/check-member")
    public ResponseEntity<Boolean> isWorkspaceMember(
            @PathVariable("workspaceId") UUID workspaceId,
            @RequestParam("userId") UUID userId) {

        log.info("Received internal membership check request for workspace: {} and user: {}", workspaceId, userId);

        boolean isMember = authService.verifyWorkspaceMembership(workspaceId, userId);
        return ResponseEntity.ok(isMember);
    }
}