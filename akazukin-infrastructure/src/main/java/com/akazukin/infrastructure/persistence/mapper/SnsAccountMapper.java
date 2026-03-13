package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.crypto.TokenEncryptor;
import com.akazukin.infrastructure.persistence.entity.SnsAccountEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SnsAccountMapper {

    private final TokenEncryptor tokenEncryptor;

    @Inject
    public SnsAccountMapper(TokenEncryptor tokenEncryptor) {
        this.tokenEncryptor = tokenEncryptor;
    }

    public SnsAccount toDomain(SnsAccountEntity entity) {
        return new SnsAccount(
                entity.id,
                entity.userId,
                SnsPlatform.valueOf(entity.platform),
                entity.accountIdentifier,
                entity.displayName,
                tokenEncryptor.decrypt(entity.accessToken),
                entity.refreshToken != null ? tokenEncryptor.decrypt(entity.refreshToken) : null,
                entity.tokenExpiresAt,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public SnsAccountEntity toEntity(SnsAccount domain) {
        SnsAccountEntity entity = new SnsAccountEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.platform = domain.getPlatform().name();
        entity.accountIdentifier = domain.getAccountIdentifier();
        entity.displayName = domain.getDisplayName();
        entity.accessToken = tokenEncryptor.encrypt(domain.getAccessToken());
        entity.refreshToken = domain.getRefreshToken() != null
                ? tokenEncryptor.encrypt(domain.getRefreshToken()) : null;
        entity.tokenExpiresAt = domain.getTokenExpiresAt();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}
