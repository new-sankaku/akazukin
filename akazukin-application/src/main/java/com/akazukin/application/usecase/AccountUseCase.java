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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AccountUseCase {

    private static final Logger LOG = Logger.getLogger(AccountUseCase.class.getName());

    private final SnsAccountRepository snsAccountRepository;
    private final SnsAdapterLookup snsAdapterLookup;

    @Inject
    public AccountUseCase(SnsAccountRepository snsAccountRepository,
                          SnsAdapterLookup snsAdapterLookup) {
        this.snsAccountRepository = snsAccountRepository;
        this.snsAdapterLookup = snsAdapterLookup;
    }

    public List<AccountResponseDto> listAccounts(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);

            return accounts.stream()
                    .map(this::toAccountResponseDto)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.listAccounts", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.listAccounts", perfMs});
            }
        }
    }

    public AccountResponseDto getAccount(UUID accountId) {
        long perfStart = System.nanoTime();
        try {
            SnsAccount account = snsAccountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));

            return toAccountResponseDto(account);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.getAccount", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.getAccount", perfMs});
            }
        }
    }

    public void deleteAccount(UUID accountId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            SnsAccount account = snsAccountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId));

            if (!account.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this account");
            }

            snsAccountRepository.deleteById(accountId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.deleteAccount", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.deleteAccount", perfMs});
            }
        }
    }

    public String getAuthorizationUrl(UUID userId, SnsPlatform platform, String callbackUrl) {
        long perfStart = System.nanoTime();
        try {
            SnsAdapter adapter = getAdapterForPlatform(platform);
            String state = UUID.randomUUID().toString();
            return adapter.getAuthorizationUrl(callbackUrl, state);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.getAuthorizationUrl", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.getAuthorizationUrl", perfMs});
            }
        }
    }

    public AccountResponseDto connectAccount(UUID userId, SnsPlatform platform,
                                             String code, String callbackUrl) {
        long perfStart = System.nanoTime();
        try {
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
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.connectAccount", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AccountUseCase.connectAccount", perfMs});
            }
        }
    }

    private SnsAdapter getAdapterForPlatform(SnsPlatform platform) {
        try {
            return snsAdapterLookup.getAdapter(platform);
        } catch (IllegalArgumentException e) {
            throw new DomainException("UNSUPPORTED_PLATFORM",
                    "Platform not supported: " + platform);
        }
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
