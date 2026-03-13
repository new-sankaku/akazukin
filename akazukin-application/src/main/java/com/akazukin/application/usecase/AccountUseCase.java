package com.akazukin.application.usecase;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountUseCase {

    private final SnsAccountRepository snsAccountRepository;

    @Inject
    public AccountUseCase(SnsAccountRepository snsAccountRepository) {
        this.snsAccountRepository = snsAccountRepository;
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
