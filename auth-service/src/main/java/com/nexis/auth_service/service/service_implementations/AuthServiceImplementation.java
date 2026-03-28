package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.ProviderType;
import com.nexis.auth_service.dto.login.LoginRequestDto;
import com.nexis.auth_service.dto.login.LoginResponseDto;
import com.nexis.auth_service.dto.logout.LogoutRequestDto;
import com.nexis.auth_service.dto.refreshtoken.RefreshTokenRequestDto;
import com.nexis.auth_service.dto.signup.SignupRequestDto;
import com.nexis.auth_service.dto.signup.SignupResponseDto;
import com.nexis.auth_service.dto.user_profile.UserProfileResponseDto;
import com.nexis.auth_service.entity.RefreshTokenEntity;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.exception.RefreshTokenNotFoundException;
import com.nexis.auth_service.exception.UserAlreadyExistsException;
import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.security.user_principal.UserPrincipal;
import com.nexis.auth_service.service.AuthService;
import com.nexis.auth_service.util.AuthUtil;
import com.nexis.auth_service.util.RefreshTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final RefreshTokenUtil refreshTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public SignupResponseDto signup(SignupRequestDto body) {
        log.info("Initiating standard email signup for: {}", body.getEmail());
        UserEntity user = signupInternal(body, ProviderType.EMAIL, null, null);
        return SignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public UserEntity signupInternal(SignupRequestDto requestDto, ProviderType providerType, String providerId, String avatarUrl) {
        UserEntity user = userRepository.findByEmail(requestDto.getEmail()).orElse(null);

        if(user != null) {
            log.warn("Signup failed: User already exists with email {}", requestDto.getEmail());

            throw new UserAlreadyExistsException("User already exists with email: " + requestDto.getEmail());
        }

        String finalAvatar = (avatarUrl != null && !avatarUrl.isBlank())
                ? avatarUrl
                : "https://api.dicebear.com/9.x/identicon/svg?seed=fallback";

        user = UserEntity.builder()
                .email(requestDto.getEmail())
                .fullname(requestDto.getFullname())
                .providerType(providerType)
                .providerId(providerId)
                .avatar(finalAvatar)
                .createdAt(LocalDateTime.now())
                .build();

        if(providerType == ProviderType.EMAIL && requestDto.getPassword() != null){
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }

        UserEntity savedUser = userRepository.save(user);
        log.info("Successfully created new user with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        log.info("Attempting login for email: {}", requestDto.getEmail());

        Authentication authentication = authenticationManager.
                authenticate(new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserEntity userEntity = principal.getUserEntity();

        RefreshTokenEntity refreshToken = refreshTokenUtil.generateRefreshToken(userEntity.getId());
        String token = authUtil.generateAccessToken(userEntity);

        log.info("User ID: {} logged in successfully.", userEntity.getId());

        return LoginResponseDto.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .jwt(token)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    @Transactional
    public ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        ProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOauth2User(oAuth2User, registrationId);
        String oauthAvatarUrl = authUtil.getAvatarFromOauth2User(oAuth2User, registrationId);

        log.info("Processing OAuth2 login for provider: {}, providerId: {}", providerType, providerId);

        UserEntity user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);
        String email = oAuth2User.getAttribute("email");
        UserEntity emailUser = userRepository.findByEmail(email).orElse(null);

        if(user == null && emailUser == null){
            log.info("No existing account found. Initiating OAuth2 signup flow.");
            String emailSignup = authUtil.determineEmailFromOauth2User(oAuth2User, registrationId, providerId);
            user = signupInternal(new SignupRequestDto(emailSignup, null, null), providerType, providerId, oauthAvatarUrl);
        } else if (user != null) {
            if(email != null && !email.isBlank() && !email.equals(user.getEmail())){
                log.info("Updating email for OAuth2 user ID: {}", user.getId());
                user.setEmail(email);
                userRepository.save(user);
            }
        } else {
            log.warn("OAuth2 login failed: Email {} is already registered with provider: {}", email, emailUser.getProviderType());
            throw new BadCredentialsException("This email is already registered with provider: " + emailUser.getProviderType());
        }

        LoginResponseDto loginResponseDto = new LoginResponseDto(
                user.getId(),
                user.getEmail(),
                authUtil.generateAccessToken(user),
                refreshTokenUtil.generateRefreshToken(user.getId()).getToken()
        );

        log.info("OAuth2 login successful for User ID: {}", user.getId());
        return ResponseEntity.ok(loginResponseDto);
    }

    @Override
    @Transactional
    public void logout(LogoutRequestDto requestDto) {
        String token = requestDto.getRefreshToken();
        log.info("Processing logout request, deleting refresh token.");
        refreshTokenUtil.deleteByToken(token);
    }

    @Transactional
    @Override
    public LoginResponseDto refreshToken(RefreshTokenRequestDto body) {
        log.info("Attempting to rotate refresh token.");

        RefreshTokenEntity refreshToken = refreshTokenUtil.findToken(body.getToken())
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found: " + body.getToken()));

        refreshToken = refreshTokenUtil.verifyAndRotate(refreshToken);
        UUID userId = refreshToken.getUserId();
        UserEntity user = userRepository.getById(userId);
        String accessToken = authUtil.generateAccessToken(user);

        log.info("Successfully rotated tokens for User ID: {}", userId);

        return LoginResponseDto.builder()
                .jwt(accessToken)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    @Override
    public UserProfileResponseDto getCurrentUserProfile(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        UserEntity user = principal.getUserEntity();

        log.info("Fetched profile for User ID: {}", user.getId());

        return UserProfileResponseDto.builder()
                .id(user.getId())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }
}