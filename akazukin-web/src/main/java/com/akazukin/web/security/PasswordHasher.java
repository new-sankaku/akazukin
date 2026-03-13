package com.akazukin.web.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Simple BCrypt-like hasher using built-in Java (for now)
// In production, use quarkus-elytron-security or jBCrypt
@ApplicationScoped
public class PasswordHasher implements com.akazukin.domain.port.PasswordHasher {
    // For MVP, using a simple hash. Replace with BCrypt later.
    public String hash(String rawPassword) {
        // TODO: Replace with proper BCrypt
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
            return saltBase64 + ":" + hashBase64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public boolean verify(String rawPassword, String hashedPassword) {
        try {
            String[] parts = hashedPassword.split(":");
            if (parts.length != 2) return false;
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] computed = md.digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computedBase64 = Base64.getEncoder().encodeToString(computed);
            return computedBase64.equals(parts[1]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify password", e);
        }
    }
}
