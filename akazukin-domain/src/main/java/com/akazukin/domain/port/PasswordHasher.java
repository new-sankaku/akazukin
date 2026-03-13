package com.akazukin.domain.port;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String hashedPassword);
}
