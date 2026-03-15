package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.infrastructure.crypto.TokenEncryptor;
import com.akazukin.infrastructure.persistence.entity.SnsAccountEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SnsAccountMapper {

    private static final Logger LOG = Logger.getLogger(SnsAccountMapper.class.getName());

    private final TokenEncryptor tokenEncryptor;

    @Inject
    public SnsAccountMapper(TokenEncryptor tokenEncryptor) {
        this.tokenEncryptor = tokenEncryptor;
    }

    public SnsAccount toDomain(SnsAccountEntity entity) {
        String accessToken = decryptSafely(entity.accessToken, entity.id, "accessToken");
        String refreshToken = entity.refreshToken != null
                ? decryptSafely(entity.refreshToken, entity.id, "refreshToken") : null;

        return new SnsAccount(
                entity.id,
                entity.userId,
                SnsPlatform.valueOf(entity.platform),
                entity.accountIdentifier,
                entity.displayName,
                accessToken,
                refreshToken,
                entity.tokenExpiresAt,
                entity.createdAt,
                entity.updatedAt
        );
    }

    private String decryptSafely(String cipherText, java.util.UUID accountId, String fieldName) {
        try {
            return tokenEncryptor.decrypt(cipherText);
        } catch (TokenEncryptor.TokenEncryptionException e) {
            LOG.log(Level.WARNING,
                    "Failed to decrypt " + fieldName + " for account " + accountId + ". Re-authentication required.",
                    e);
            return null;
        }
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
