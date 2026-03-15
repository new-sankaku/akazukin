package com.akazukin.application.usecase;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountUseCaseTest {

    private InMemorySnsAccountRepository snsAccountRepository;
    private AccountUseCase accountUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        snsAccountRepository = new InMemorySnsAccountRepository();
        accountUseCase = new AccountUseCase(snsAccountRepository, platform -> {
            throw new UnsupportedOperationException("Not used in this test");
        });
        userId = UUID.randomUUID();
    }

    private SnsAccount createAndSaveAccount(UUID ownerId, SnsPlatform platform, String identifier) {
        Instant now = Instant.now();
        SnsAccount account = new SnsAccount(
                UUID.randomUUID(), ownerId, platform, identifier, "Display " + identifier,
                "access_token", "refresh_token", now.plusSeconds(3600), now, now
        );
        return snsAccountRepository.save(account);
    }

    @Test
    void listAccounts_returnsAccountsForUser() {
        createAndSaveAccount(userId, SnsPlatform.TWITTER, "@user");
        createAndSaveAccount(userId, SnsPlatform.BLUESKY, "user.bsky.social");

        UUID otherUserId = UUID.randomUUID();
        createAndSaveAccount(otherUserId, SnsPlatform.MASTODON, "@other@mastodon.social");

        List<AccountResponseDto> result = accountUseCase.listAccounts(userId);

        assertEquals(2, result.size());
    }

    @Test
    void listAccounts_returnsEmptyListForUserWithNoAccounts() {
        List<AccountResponseDto> result = accountUseCase.listAccounts(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void getAccount_returnsExistingAccount() {
        SnsAccount saved = createAndSaveAccount(userId, SnsPlatform.TWITTER, "@testuser");

        AccountResponseDto result = accountUseCase.getAccount(saved.getId());

        assertNotNull(result);
        assertEquals(saved.getId(), result.id());
        assertEquals("TWITTER", result.platform());
        assertEquals("@testuser", result.accountIdentifier());
    }

    @Test
    void getAccount_throwsAccountNotFoundExceptionForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(AccountNotFoundException.class,
                () -> accountUseCase.getAccount(nonExistentId));
    }

    @Test
    void deleteAccount_removesAccount() {
        SnsAccount saved = createAndSaveAccount(userId, SnsPlatform.TWITTER, "@testuser");

        accountUseCase.deleteAccount(saved.getId(), userId);

        assertThrows(AccountNotFoundException.class,
                () -> accountUseCase.getAccount(saved.getId()));
    }

    @Test
    void deleteAccount_throwsForbiddenWhenNotOwner() {
        SnsAccount saved = createAndSaveAccount(userId, SnsPlatform.TWITTER, "@testuser");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> accountUseCase.deleteAccount(saved.getId(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void deleteAccount_throwsAccountNotFoundForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(AccountNotFoundException.class,
                () -> accountUseCase.deleteAccount(nonExistentId, userId));
    }

    @Test
    void listAccounts_returnsDtoWithCorrectFields() {
        SnsAccount saved = createAndSaveAccount(userId, SnsPlatform.TWITTER, "@testuser");

        List<AccountResponseDto> result = accountUseCase.listAccounts(userId);

        assertEquals(1, result.size());
        AccountResponseDto dto = result.get(0);
        assertEquals(saved.getId(), dto.id());
        assertEquals("TWITTER", dto.platform());
        assertEquals("@testuser", dto.accountIdentifier());
        assertEquals("Display @testuser", dto.displayName());
        assertNotNull(dto.connectedAt());
    }

    // --- In-memory implementation ---

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

        @Override
        public List<SnsAccount> findAllByIds(Collection<UUID> ids) {
            return store.values().stream()
                    .filter(account -> ids.contains(account.getId()))
                    .toList();
        }

        @Override
        public long countByPlatform(SnsPlatform platform) {
            return store.values().stream()
                    .filter(account -> account.getPlatform() == platform)
                    .count();
        }

        @Override
        public long countAll() {
            return store.size();
        }
    }
}
