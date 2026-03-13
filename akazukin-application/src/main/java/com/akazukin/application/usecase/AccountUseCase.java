package com.akazukin.application.usecase;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AccountUseCase {

    private final SnsAccountRepository snsAccountRepository;
    private final Map<SnsPlatform, SnsAdapter> snsAdapters = new ConcurrentHashMap<>();

    @Inject
    public AccountUseCase(SnsAccountRepository snsAccountRepository) {
        this.snsAccountRepository = snsAccountRepository;
    }

    public void registerAdapter(SnsAdapter adapter) {
        snsAdapters.put(adapter.platform(), adapter);
    }

    public List<AccountResponseDto> listAccounts(UUID userId) {
        List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);

        return accounts.stream()
                .map(this::toAccountResponseDto)
                .toList();
    }

    public AccountResponseDto getAccount(UUID accountId) {
        SnsAccount account = snsAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return toAccountResponseDto(account);
    }

    public void deleteAccount(UUID accountId, UUID userId) {
        SnsAccount account = snsAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!account.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this account");
        }

        snsAccountRepository.deleteById(accountId);
    }

    public String getAuthorizationUrl(UUID userId, SnsPlatform platform, String callbackUrl) {
        SnsAdapter adapter = getAdapterForPlatform(platform);
        String state = UUID.randomUUID().toString();
        return adapter.getAuthorizationUrl(callbackUrl, state);
    }

    public AccountResponseDto connectAccount(UUID userId, SnsPlatform platform,
                                             String code, String callbackUrl) {
        SnsAdapter adapter = getAdapterForPlatform(platform);

        SnsAuthToken token = adapter.exchangeToken(code, callbackUrl);
        SnsProfile profile = adapter.getProfile(token.accessToken());

        SnsAccount existingAccount = snsAccountRepository
                .findByUserIdAndPlatform(userId, platform)
                .orElse(null);

        Instant now = Instant.now();
        SnsAccount account;

        if (existingAccount != null) {
            existingAccount.setAccessToken(token.accessToken());
            existingAccount.setRefreshToken(token.refreshToken());
            existingAccount.setTokenExpiresAt(token.expiresAt());
            existingAccount.setAccountIdentifier(profile.accountIdentifier());
            existingAccount.setDisplayName(profile.displayName());
            existingAccount.setUpdatedAt(now);
            account = snsAccountRepository.save(existingAccount);
        } else {
            account = new SnsAccount(
                    null,
                    userId,
                    platform,
                    profile.accountIdentifier(),
                    profile.displayName(),
                    token.accessToken(),
                    token.refreshToken(),
                    token.expiresAt(),
                    now,
                    now
            );
            account = snsAccountRepository.save(account);
        }

        return toAccountResponseDto(account);
    }

    private SnsAdapter getAdapterForPlatform(SnsPlatform platform) {
        SnsAdapter adapter = snsAdapters.get(platform);
        if (adapter == null) {
            throw new DomainException("UNSUPPORTED_PLATFORM",
                    "Platform not supported: " + platform);
        }
        return adapter;
    }

    private AccountResponseDto toAccountResponseDto(SnsAccount account) {
        return new AccountResponseDto(
                account.getId(),
                account.getPlatform().name(),
                account.getAccountIdentifier(),
                account.getDisplayName(),
                account.getCreatedAt()
        );
    }
}
