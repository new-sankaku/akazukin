package com.akazukin.application.usecase;

import com.akazukin.application.dto.RegisterRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.PasswordHasher;
import com.akazukin.domain.port.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthUseCaseTest {

    private InMemoryUserRepository userRepository;
    private SimplePasswordHasher passwordHasher;
    private AuthUseCase authUseCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordHasher = new SimplePasswordHasher();
        authUseCase = new AuthUseCase(userRepository, passwordHasher);
    }

    @Test
    void register_createsUserWithHashedPassword() {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "test@example.com", "password123");

        User result = authUseCase.register(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("hashed:password123", result.getPasswordHash());
    }

    @Test
    void register_throwsDuplicateUserWhenUsernameExists() {
        RegisterRequestDto first = new RegisterRequestDto("testuser", "first@example.com", "password");
        authUseCase.register(first);

        RegisterRequestDto duplicate = new RegisterRequestDto("testuser", "second@example.com", "password");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(duplicate));
        assertEquals("DUPLICATE_USER", exception.getErrorCode());
    }

    @Test
    void register_throwsDuplicateUserWhenEmailExists() {
        RegisterRequestDto first = new RegisterRequestDto("user1", "test@example.com", "password");
        authUseCase.register(first);

        RegisterRequestDto duplicate = new RegisterRequestDto("user2", "test@example.com", "password");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(duplicate));
        assertEquals("DUPLICATE_USER", exception.getErrorCode());
    }

    @Test
    void register_throwsInvalidInputWhenUsernameIsBlank() {
        RegisterRequestDto request = new RegisterRequestDto("", "test@example.com", "password");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void register_throwsInvalidInputWhenUsernameIsNull() {
        RegisterRequestDto request = new RegisterRequestDto(null, "test@example.com", "password");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void register_throwsInvalidInputWhenEmailIsBlank() {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "", "password");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void register_throwsInvalidInputWhenPasswordIsBlank() {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "test@example.com", "");

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.register(request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void authenticate_successfulWithCorrectPassword() {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "test@example.com", "password123");
        authUseCase.register(request);

        User result = authUseCase.authenticate("testuser", "password123");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void authenticate_throwsInvalidCredentialsForWrongPassword() {
        RegisterRequestDto request = new RegisterRequestDto("testuser", "test@example.com", "password123");
        authUseCase.register(request);

        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.authenticate("testuser", "wrongpassword"));
        assertEquals("INVALID_CREDENTIALS", exception.getErrorCode());
    }

    @Test
    void authenticate_throwsInvalidCredentialsForNonExistentUser() {
        DomainException exception = assertThrows(DomainException.class,
                () -> authUseCase.authenticate("nonexistent", "password"));
        assertEquals("INVALID_CREDENTIALS", exception.getErrorCode());
    }

    /**
     * Simple in-memory UserRepository for testing.
     */
    private static class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> store = new HashMap<>();

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public List<User> findAllByIds(Collection<UUID> ids) {
            return ids.stream()
                    .map(store::get)
                    .filter(user -> user != null)
                    .toList();
        }

        @Override
        public User save(User user) {
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }

    /**
     * Simple password hasher that prepends "hashed:" for testing.
     */
    private static class SimplePasswordHasher implements PasswordHasher {

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean verify(String rawPassword, String hashedPassword) {
            return hashedPassword.equals("hashed:" + rawPassword);
        }
    }
}
