package com.akazukin.domain.exception;

import java.util.UUID;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(UUID accountId) {
        super("ACCOUNT_NOT_FOUND", "Account not found: " + accountId);
    }
}
