package com.akazukin.application.usecase;

import com.akazukin.application.dto.RegisterRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.PasswordHasher;
import com.akazukin.domain.port.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Inject
    public AuthUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(RegisterRequestDto request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Username is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Password is required");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DomainException("DUPLICATE_USERNAME", "Username already exists: " + request.username());
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DomainException("DUPLICATE_EMAIL", "Email already exists: " + request.email());
        }

        Instant now = Instant.now();
        String hashedPassword = passwordHasher.hash(request.password());

        User user = new User(
                UUID.randomUUID(),
                request.username(),
                request.email(),
                hashedPassword,
                Role.ADMIN,
                now,
                now
        );

        return userRepository.save(user);
    }

    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Invalid username or password"));

        if (!passwordHasher.verify(rawPassword, user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        return user;
    }
}
