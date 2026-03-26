package com.nexis.auth_service.repository;

import com.nexis.auth_service.config.type.ProviderType;
import com.nexis.auth_service.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByProviderIdAndProviderType(String providerId, ProviderType providerType);
}
