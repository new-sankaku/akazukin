package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SnsAccount {

    private UUID id;
    private UUID userId;
    private SnsPlatform platform;
    private String accountIdentifier;
    private String displayName;
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public SnsAccount(UUID id, UUID userId, SnsPlatform platform, String accountIdentifier,
                      String displayName, String accessToken, String refreshToken,
                      Instant tokenExpiresAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.accountIdentifier = accountIdentifier;
        this.displayName = displayName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public SnsPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SnsPlatform platform) {
        this.platform = platform;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public void setAccountIdentifier(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnsAccount that = (SnsAccount) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SnsAccount{" +
                "id=" + id +
                ", userId=" + userId +
                ", platform=" + platform +
                ", accountIdentifier='" + accountIdentifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", tokenExpiresAt=" + tokenExpiresAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
