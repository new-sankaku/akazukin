package com.akazukin.adapter.threads;

import com.akazukin.domain.model.SnsPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ThreadsAdapter Unit Tests")
class ThreadsAdapterTest {

    private ThreadsAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ThreadsAdapter("test-client-id", "test-client-secret");
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns THREADS")
        void returnsThreads() {
            assertEquals(SnsPlatform.THREADS, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 500")
        void returnsDefault() {
            assertEquals(500, adapter.getMaxContentLength());
        }
    }

    @Nested
    @DisplayName("getAuthorizationUrl()")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("contains client_id")
        void containsClientId() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("client_id=test-client-id"));
        }

        @Test
        @DisplayName("contains response_type=code")
        void containsResponseType() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("response_type=code"));
        }

        @Test
        @DisplayName("contains state")
        void containsState() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "mystate");
            assertTrue(url.contains("state=mystate"));
        }

        @Test
        @DisplayName("contains redirect_uri")
        void containsRedirectUri() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("redirect_uri="));
        }

        @Test
        @DisplayName("contains scope with threads permissions")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("starts with Threads OAuth URL")
        void startsWithThreadsUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith("https://threads.net/oauth/authorize"));
        }

        @Test
        @DisplayName("URL is not null for any inputs")
        void notNullForAnyInputs() {
            assertNotNull(adapter.getAuthorizationUrl("https://example.com", "s"));
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("clientId must not be null")
        void clientIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new ThreadsAdapter(null, "secret"));
        }

        @Test
        @DisplayName("clientSecret must not be null")
        void clientSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new ThreadsAdapter("id", null));
        }

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }
    }
}
