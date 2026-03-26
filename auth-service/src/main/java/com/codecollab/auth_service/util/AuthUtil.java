package com.codecollab.auth_service.util;


import com.codecollab.auth_service.entity.UserEntity;
import com.codecollab.auth_service.config.type.ProviderType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Slf4j
@Component
public class AuthUtil {

    @Value("${jwt.secretkey}")
    private String jwtSecretKey;

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserEntity body){
        return Jwts.builder()
                .subject(body.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+1000*60*15))
                .signWith(getSecretKey())     // signature create
                .claim("userId",body.getId().toString())     // custom payload field
                .compact();

    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }


    public ProviderType getProviderTypeFromRegistrationId(String registrationId){
        return switch (registrationId.toLowerCase() ) {
            case "google" -> ProviderType.GOOGLE;
            case ("github") -> ProviderType.GITHUB;
            default -> throw new IllegalArgumentException("Unsupported oauth provider :"+registrationId);
        };
    }

    public String determineProviderIdFromOauth2User(OAuth2User oAuth2User , String registrationId){
        String providerId = switch (registrationId.toLowerCase()){
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> oAuth2User.getAttribute("id").toString();
            default -> {
                log.error("Unsupported oauth2 provider: {}",registrationId);
                throw new IllegalArgumentException("Unsupported oauth2 provider"+registrationId);
            }
        };

        if(providerId==null || providerId.isBlank()){
            log.error("Unable to determine provider id for provider : {}",registrationId);
            throw new IllegalArgumentException("Unable to determine provider id for Oauth user");
        }
        return providerId;
    }

    public String determineEmailFromOauth2User(OAuth2User oAuth2User,String registrationId,String providerId){
        String email = oAuth2User.getAttribute("email");
        if(email!=null && !email.isBlank()) return email;

        return switch (registrationId.toLowerCase()){
            case "google"->oAuth2User.getAttribute("sub");
            case "github"->oAuth2User.getAttribute("login");
            default -> providerId;
        };
    }
}