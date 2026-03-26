package com.nexis.auth_service.service.service_implementations;

import com.nexis.auth_service.config.type.ProviderType;
import com.nexis.auth_service.dto.login.LoginRequestDto;
import com.nexis.auth_service.dto.login.LoginResponseDto;
import com.nexis.auth_service.dto.*;
import com.nexis.auth_service.dto.signup.SignupRequestDto;
import com.nexis.auth_service.dto.signup.SignupResponseDto;
import com.nexis.auth_service.entity.UserEntity;

import com.nexis.auth_service.repository.UserRepository;
import com.nexis.auth_service.service.AuthService;
import com.nexis.auth_service.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImplementation implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    //Controller
    @Override
    public SignupResponseDto signup(SignupRequestDto body) {
        UserEntity user = signupInternal(body,ProviderType.EMAIL,null);
        return SignupResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }


    //INTERNAL SIGNUP!!
    @Transactional
    public UserEntity signupInternal(SignupRequestDto requestDto, ProviderType providerType, String providerId) {
        //1. Check if user is already account or not
        UserEntity user = userRepository.findByEmail(requestDto.getEmail()).orElse(null);

        if(user!=null)
            throw new IllegalArgumentException("User already exists");

        //2. Create new User
        user = UserEntity.builder()
                .email(requestDto.getEmail())
                .fullname(requestDto.getFullname())
                .providerType(providerType)
                .providerId(providerId)
                .createdAt(LocalDateTime.now())
                .build();

        //3. Encode Password
        if(providerType == ProviderType.EMAIL && requestDto.getPassword() != null){
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }

        //4. Save and return
        return userRepository.save(user);
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto) {
        // 1. AuthenticationManager delegates to AuthenticationProvider
        Authentication authentication = authenticationManager.
                authenticate(new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword()));

        //Principals() -> Username and details
        //Credentials() -> Password
        //Details() -> session id and ip address

        // Actually, AuthenticationManager → ProviderManager → DaoAuthenticationProvider
        // DaoAuthenticationProvider uses UserDetailsService + PasswordEncoder

        // 2. Principal is the authenticated user (UserDetails implementation)
        UserEntity userEntity = (UserEntity) authentication.getPrincipal();

        //3. Generate Access token for it
        String token = authUtil.generateAccessToken(userEntity);

        return LoginResponseDto.builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .jwt(token)
                .build();
    }

    @Override
    public ResponseEntity<LoginResponseDto> handleOauth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        // Find Provider type and id
        //save the provider type and id Info with user
        //If user has an account -> directly login
        // if not -> signup -> login

        ProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOauth2User(oAuth2User,registrationId);

        UserEntity user = userRepository.findByProviderIdAndProviderType(providerId,providerType).orElse(null);

        String email = oAuth2User.getAttribute("email");

        UserEntity emailUser = userRepository.findByEmail(email).orElse(null);

        if(user == null && emailUser == null){
            //signup flow:
            String emailSignup = authUtil.determineEmailFromOauth2User(oAuth2User,registrationId,providerId);

            user = signupInternal(new SignupRequestDto(emailSignup, null, null),providerType,providerId);
        } else if (user!=null) {
            if(email!=null && !email.isBlank() && !email.equals(user.getEmail())){
                user.setEmail(email);
                userRepository.save(user);
            }
        }
        else {
            throw new BadCredentialsException("This email is already registered with provider : "+emailUser.getProviderType());
        }

        LoginResponseDto loginResponseDto = new LoginResponseDto(user.getId(), user.getEmail(), authUtil.generateAccessToken(user));

        return ResponseEntity.ok(loginResponseDto);
    }

    @Override
    public void deleteAccount(LoginRequestDto requestDto) {

    }

    @Override
    public void logout(LoginRequestDto requestDto) {

    }
}
