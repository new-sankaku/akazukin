package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.persistence.entity.SnsAccountEntity;

public final class SnsAccountMapper {
    private SnsAccountMapper() {}

    public static SnsAccount toDomain(SnsAccountEntity entity) {
        return new SnsAccount(
                entity.id,
                entity.userId,
                SnsPlatform.valueOf(entity.platform),
                entity.accountIdentifier,
                entity.displayName,
                entity.accessToken,
                entity.refreshToken,
                entity.tokenExpiresAt,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static SnsAccountEntity toEntity(SnsAccount domain) {
        SnsAccountEntity entity = new SnsAccountEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.platform = domain.getPlatform().name();
        entity.accountIdentifier = domain.getAccountIdentifier();
        entity.displayName = domain.getDisplayName();
        entity.accessToken = domain.getAccessToken();
        entity.refreshToken = domain.getRefreshToken();
        entity.tokenExpiresAt = domain.getTokenExpiresAt();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}
