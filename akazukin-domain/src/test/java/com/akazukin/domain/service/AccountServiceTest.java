package com.akazukin.domain.service;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    private InMemorySnsAccountRepository accountRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemorySnsAccountRepository();
        accountService = new AccountService(accountRepository);
    }

    @Test
    void constructor_throwsWhenRepositoryIsNull() {
        assertThrows(NullPointerException.class, () -> new AccountService(null));
    }

    @Test
    void linkAccount_createsNewAccount() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        SnsAccount result = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@testuser", "Test User", token);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(SnsPlatform.TWITTER, result.getPlatform());
        assertEquals("@testuser", result.getAccountIdentifier());
        assertEquals("Test User", result.getDisplayName());
        assertEquals("access", result.getAccessToken());
        assertEquals("refresh", result.getRefreshToken());
    }

    @Test
    void linkAccount_throwsWhenUserAlreadyHasPlatformAccount() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        accountService.linkAccount(userId, SnsPlatform.TWITTER, "@user1", "User 1", token);

        assertThrows(IllegalStateException.class,
                () -> accountService.linkAccount(userId, SnsPlatform.TWITTER,
                        "@user2", "User 2", token));
    }

    @Test
    void linkAccount_allowsDifferentPlatformsForSameUser() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        SnsAccount twitter = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@user", "User", token);
        SnsAccount bluesky = accountService.linkAccount(userId, SnsPlatform.BLUESKY,
                "user.bsky.social", "User", token);

        assertNotNull(twitter);
        assertNotNull(bluesky);
        assertEquals(SnsPlatform.TWITTER, twitter.getPlatform());
        assertEquals(SnsPlatform.BLUESKY, bluesky.getPlatform());
    }

    @Test
    void linkAccount_throwsWhenUserIdIsNull() {
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        assertThrows(NullPointerException.class,
                () -> accountService.linkAccount(null, SnsPlatform.TWITTER,
                        "@user", "User", token));
    }

    @Test
    void linkAccount_throwsWhenPlatformIsNull() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        assertThrows(NullPointerException.class,
                () -> accountService.linkAccount(userId, null,
                        "@user", "User", token));
    }

    @Test
    void linkAccount_throwsWhenAccountIdentifierIsNull() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        assertThrows(NullPointerException.class,
                () -> accountService.linkAccount(userId, SnsPlatform.TWITTER,
                        null, "User", token));
    }

    @Test
    void linkAccount_throwsWhenTokenIsNull() {
        UUID userId = UUID.randomUUID();

        assertThrows(NullPointerException.class,
                () -> accountService.linkAccount(userId, SnsPlatform.TWITTER,
                        "@user", "User", null));
    }

    @Test
    void getAccount_returnsExistingAccount() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");
        SnsAccount created = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@user", "User", token);

        SnsAccount found = accountService.getAccount(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(SnsPlatform.TWITTER, found.getPlatform());
    }

    @Test
    void getAccount_throwsAccountNotFoundExceptionForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccount(nonExistentId));
    }

    @Test
    void getAccount_throwsWhenAccountIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> accountService.getAccount(null));
    }

    @Test
    void getAccountsByUser_returnsAllUserAccounts() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        accountService.linkAccount(userId, SnsPlatform.TWITTER, "@user", "User", token);
        accountService.linkAccount(userId, SnsPlatform.BLUESKY, "user.bsky.social", "User", token);

        List<SnsAccount> accounts = accountService.getAccountsByUser(userId);

        assertEquals(2, accounts.size());
    }

    @Test
    void getAccountsByUser_returnsEmptyListForUserWithNoAccounts() {
        UUID userId = UUID.randomUUID();

        List<SnsAccount> accounts = accountService.getAccountsByUser(userId);

        assertTrue(accounts.isEmpty());
    }

    @Test
    void getAccountsByUser_throwsWhenUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> accountService.getAccountsByUser(null));
    }

    @Test
    void updateToken_updatesTokenFields() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken originalToken = new SnsAuthToken("old_access", "old_refresh",
                Instant.now().plusSeconds(3600), "read write");
        SnsAccount created = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@user", "User", originalToken);

        SnsAuthToken newToken = new SnsAuthToken("new_access", "new_refresh",
                Instant.now().plusSeconds(7200), "read write");

        SnsAccount updated = accountService.updateToken(created.getId(), newToken);

        assertEquals("new_access", updated.getAccessToken());
        assertEquals("new_refresh", updated.getRefreshToken());
        assertEquals(newToken.expiresAt(), updated.getTokenExpiresAt());
    }

    @Test
    void updateToken_throwsWhenTokenIsNull() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");
        SnsAccount created = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@user", "User", token);

        assertThrows(NullPointerException.class,
                () -> accountService.updateToken(created.getId(), null));
    }

    @Test
    void updateToken_throwsWhenAccountNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateToken(nonExistentId, token));
    }

    @Test
    void unlinkAccount_removesAccount() {
        UUID userId = UUID.randomUUID();
        SnsAuthToken token = new SnsAuthToken("access", "refresh",
                Instant.now().plusSeconds(3600), "read write");
        SnsAccount created = accountService.linkAccount(userId, SnsPlatform.TWITTER,
                "@user", "User", token);

        accountService.unlinkAccount(created.getId());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccount(created.getId()));
    }

    @Test
    void unlinkAccount_throwsWhenAccountNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(AccountNotFoundException.class,
                () -> accountService.unlinkAccount(nonExistentId));
    }

    /**
     * Simple in-memory SnsAccountRepository for testing.
     */
    private static class InMemorySnsAccountRepository implements SnsAccountRepository {

        private final Map<UUID, SnsAccount> store = new HashMap<>();

        @Override
        public Optional<SnsAccount> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<SnsAccount> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(account -> account.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
            return store.values().stream()
                    .filter(account -> account.getUserId().equals(userId))
                    .filter(account -> account.getPlatform() == platform)
                    .findFirst();
        }

        @Override
        public SnsAccount save(SnsAccount snsAccount) {
            store.put(snsAccount.getId(), snsAccount);
            return snsAccount;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
