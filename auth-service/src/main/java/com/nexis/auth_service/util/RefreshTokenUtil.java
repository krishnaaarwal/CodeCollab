package com.nexis.auth_service.util;

import com.nexis.auth_service.entity.RefreshTokenEntity;
import com.nexis.auth_service.entity.UserEntity;
import com.nexis.auth_service.exception.RefreshTokenExpiredException;
import com.nexis.auth_service.repository.RefreshTokenRepository;
import com.nexis.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenUtil {

        private final RefreshTokenRepository refreshTokenRepository;
        private final UserRepository userRepository;

        public RefreshTokenEntity generateRefreshToken(UUID userId) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

            RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                    .userId(user.getId())
                    .token(UUID.randomUUID().toString())
                    .expiredAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            return refreshTokenRepository.save(refreshToken);
        }

        public Optional<RefreshTokenEntity> findToken(String token) {
            return refreshTokenRepository.findByToken(token);
        }

        public RefreshTokenEntity verifyAndRotate(RefreshTokenEntity token) {
            if (token.getExpiredAt().isBefore(Instant.now())) {
                refreshTokenRepository.delete(token);
                throw new RefreshTokenExpiredException("Refresh token expired: " + token.getToken());
            }

            RefreshTokenEntity refreshTokenEntity = generateRefreshToken(token.getUserId());
            refreshTokenRepository.delete(token);
            return refreshTokenEntity;

        }

        public void deleteByToken(String token) {
            RefreshTokenEntity entity =  refreshTokenRepository.findByToken(token).orElse(null);
            if(entity!=null){
                refreshTokenRepository.delete(entity);
            }
        }

}
