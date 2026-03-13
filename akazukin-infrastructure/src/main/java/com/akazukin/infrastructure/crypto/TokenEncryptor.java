package com.akazukin.infrastructure.crypto;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenEncryptor {

    public String encrypt(String plainText) {
        // TODO: Implement encryption (e.g., AES-GCM)
        throw new UnsupportedOperationException("Token encryption not yet implemented");
    }

    public String decrypt(String cipherText) {
        // TODO: Implement decryption
        throw new UnsupportedOperationException("Token decryption not yet implemented");
    }
}
