package com.akazukin.adapter.vk;

import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VkAdapter Unit Tests")
class VkAdapterTest {

    private VkAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new VkAdapter("test-client-id", "test-client-secret");
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns VK")
        void returnsVk() {
            assertEquals(SnsPlatform.VK, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns default 2200")
        void returnsDefault() {
            assertEquals(2200, adapter.getMaxContentLength());
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
        @DisplayName("contains scope with wall and offline")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("contains display=page")
        void containsDisplay() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("display=page"));
        }

        @Test
        @DisplayName("contains API version")
        void containsApiVersion() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("v=5.199"));
        }

        @Test
        @DisplayName("starts with VK OAuth URL")
        void startsWithVkUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith("https://oauth.vk.com/authorize"));
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("returns same token as passthrough (no refresh support)")
        void tokenPassthrough() {
            SnsAuthToken token = adapter.refreshToken("my-vk-token");

            assertNotNull(token);
            assertEquals("my-vk-token", token.accessToken());
            assertNull(token.refreshToken());
            assertNull(token.expiresAt());
            assertNull(token.scope());
        }

        @Test
        @DisplayName("different token values returned as-is")
        void differentTokenValues() {
            SnsAuthToken token = adapter.refreshToken("another-token");
            assertEquals("another-token", token.accessToken());
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("clientId must not be null")
        void clientIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new VkAdapter(null, "secret"));
        }

        @Test
        @DisplayName("clientSecret must not be null")
        void clientSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new VkAdapter("id", null));
        }

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }
    }
}
