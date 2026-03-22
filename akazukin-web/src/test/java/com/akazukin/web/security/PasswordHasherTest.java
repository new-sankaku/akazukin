package com.akazukin.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordHasher Tests")
class PasswordHasherTest {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
    }

    @Nested
    @DisplayName("hash")
    class HashTests {

        @Test
        @DisplayName("Returns a BCrypt hash string")
        void hash_returnsNonNullBcryptString() {
            String hashed = hasher.hash("password123");

            assertNotNull(hashed);
            assertTrue(hashed.startsWith("$2a$"));
        }

        @Test
        @DisplayName("Same password produces different hashes due to random salt")
        void hash_samePasswordProducesDifferentHashes() {
            String hash1 = hasher.hash("password123");
            String hash2 = hasher.hash("password123");

            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Null password throws NullPointerException")
        void hash_nullPasswordThrows() {
            assertThrows(NullPointerException.class, () -> hasher.hash(null));
        }

        @Test
        @DisplayName("Empty password produces a valid hash")
        void hash_emptyPasswordProducesHash() {
            String hashed = hasher.hash("");

            assertNotNull(hashed);
            assertTrue(hashed.startsWith("$2a$"));
        }
    }

    @Nested
    @DisplayName("verify")
    class VerifyTests {

        @Test
        @DisplayName("Correct password passes verification")
        void verify_correctPasswordReturnsTrue() {
            String hashed = hasher.hash("password123");

            assertTrue(hasher.verify("password123", hashed));
        }

        @Test
        @DisplayName("Wrong password fails verification")
        void verify_wrongPasswordReturnsFalse() {
            String hashed = hasher.hash("password123");

            assertFalse(hasher.verify("wrongpassword", hashed));
        }

        @Test
        @DisplayName("Empty password fails against non-empty hash")
        void verify_emptyPasswordAgainstNonEmptyHash() {
            String hashed = hasher.hash("password123");

            assertFalse(hasher.verify("", hashed));
        }

        @Test
        @DisplayName("Correct empty password passes verification")
        void verify_emptyPasswordMatchesEmptyHash() {
            String hashed = hasher.hash("");

            assertTrue(hasher.verify("", hashed));
        }

        @Test
        @DisplayName("Null raw password throws NullPointerException")
        void verify_nullRawPasswordThrows() {
            String hashed = hasher.hash("password123");

            assertThrows(NullPointerException.class, () -> hasher.verify(null, hashed));
        }

        @Test
        @DisplayName("Null hashed password throws exception")
        void verify_nullHashedPasswordThrows() {
            assertThrows(Exception.class, () -> hasher.verify("password123", null));
        }
    }
}
