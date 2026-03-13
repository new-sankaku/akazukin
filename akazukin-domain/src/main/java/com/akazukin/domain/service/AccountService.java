package com.akazukin.domain.service;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAccountRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AccountService {

    private final SnsAccountRepository snsAccountRepository;

    public AccountService(SnsAccountRepository snsAccountRepository) {
        this.snsAccountRepository = Objects.requireNonNull(snsAccountRepository, "snsAccountRepository must not be null");
    }

    public SnsAccount linkAccount(UUID userId, SnsPlatform platform, String accountIdentifier,
                                  String displayName, SnsAuthToken token) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(platform, "platform must not be null");
        Objects.requireNonNull(accountIdentifier, "accountIdentifier must not be null");
        Objects.requireNonNull(token, "token must not be null");

        snsAccountRepository.findByUserIdAndPlatform(userId, platform)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "User already has an account for platform: " + platform);
                });

        Instant now = Instant.now();
        SnsAccount account = new SnsAccount(
                UUID.randomUUID(),
                userId,
                platform,
                accountIdentifier,
                displayName,
                token.accessToken(),
                token.refreshToken(),
                token.expiresAt(),
                now,
                now
        );

        return snsAccountRepository.save(account);
    }

    public SnsAccount getAccount(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return snsAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    public List<SnsAccount> getAccountsByUser(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return snsAccountRepository.findByUserId(userId);
    }

    public SnsAccount updateToken(UUID accountId, SnsAuthToken token) {
        Objects.requireNonNull(token, "token must not be null");

        SnsAccount account = getAccount(accountId);
        account.setAccessToken(token.accessToken());
        account.setRefreshToken(token.refreshToken());
        account.setTokenExpiresAt(token.expiresAt());
        account.setUpdatedAt(Instant.now());

        return snsAccountRepository.save(account);
    }

    public void unlinkAccount(UUID accountId) {
        getAccount(accountId);
        snsAccountRepository.deleteById(accountId);
    }
}
