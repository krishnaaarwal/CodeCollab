package com.codecollab.auth_service.repository;

import com.codecollab.auth_service.entity.UserEntity;
import com.codecollab.auth_service.config.type.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByProviderIdAndProviderType(String providerId, ProviderType providerType);
}
