package com.akazukin.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserTest {

    private User createUser(UUID id) {
        Instant now = Instant.now();
        return new User(id, "testuser", "test@example.com", "hashed_password_123",
                Role.USER, now, now);
    }

    @Test
    void equals_returnsTrueForSameId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user1 = new User(id, "user1", "user1@example.com", "hash1",
                Role.USER, now, now);
        User user2 = new User(id, "user2", "user2@example.com", "hash2",
                Role.ADMIN, now, now);

        assertEquals(user1, user2);
    }

    @Test
    void equals_returnsFalseForDifferentId() {
        User user1 = createUser(UUID.randomUUID());
        User user2 = createUser(UUID.randomUUID());

        assertNotEquals(user1, user2);
    }

    @Test
    void hashCode_sameForSameId() {
        UUID id = UUID.randomUUID();

        User user1 = createUser(id);
        User user2 = createUser(id);

        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void equals_returnsFalseForNull() {
        User user = createUser(UUID.randomUUID());

        assertNotEquals(null, user);
    }

    @Test
    void equals_returnsTrueForSameInstance() {
        User user = createUser(UUID.randomUUID());

        assertEquals(user, user);
    }

    @Test
    void toString_doesNotIncludePasswordHash() {
        User user = createUser(UUID.randomUUID());

        String result = user.toString();

        assertFalse(result.contains("hashed_password_123"),
                "toString should not include passwordHash");
        assertFalse(result.contains("passwordHash"),
                "toString should not contain passwordHash field name");
    }

    @Test
    void toString_includesOtherFields() {
        User user = createUser(UUID.randomUUID());

        String result = user.toString();

        assertTrue(result.contains("testuser"));
        assertTrue(result.contains("test@example.com"));
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
