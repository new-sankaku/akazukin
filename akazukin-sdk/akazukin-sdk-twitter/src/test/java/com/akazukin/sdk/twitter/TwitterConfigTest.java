package com.akazukin.sdk.twitter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TwitterConfigTest {

    @Test
    void constructor_throwsWhenClientIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig(null, "secret", "https://example.com/callback"));
    }

    @Test
    void constructor_throwsWhenClientIdIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("  ", "secret", "https://example.com/callback"));
    }

    @Test
    void constructor_throwsWhenClientIdIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("", "secret", "https://example.com/callback"));
    }

    @Test
    void constructor_throwsWhenClientSecretIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("clientId", null, "https://example.com/callback"));
    }

    @Test
    void constructor_throwsWhenClientSecretIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("clientId", "  ", "https://example.com/callback"));
    }

    @Test
    void constructor_createsValidConfig() {
        TwitterConfig config = new TwitterConfig("myClientId", "mySecret", "https://example.com/callback");

        assertNotNull(config);
        assertEquals("myClientId", config.clientId());
        assertEquals("mySecret", config.clientSecret());
        assertEquals("https://example.com/callback", config.redirectUri());
    }

    @Test
    void constructor_allowsNullRedirectUri() {
        TwitterConfig config = new TwitterConfig("clientId", "secret", null);

        assertNotNull(config);
        assertEquals("clientId", config.clientId());
        assertEquals("secret", config.clientSecret());
    }
}
