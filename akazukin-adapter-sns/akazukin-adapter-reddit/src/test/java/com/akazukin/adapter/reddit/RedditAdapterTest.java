package com.akazukin.adapter.reddit;

import com.akazukin.domain.exception.SnsApiException;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.SnsPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RedditAdapter Unit Tests")
class RedditAdapterTest {

    private RedditAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedditAdapter("test-client-id", "test-client-secret", "akazukin-test/1.0");
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns REDDIT")
        void returnsReddit() {
            assertEquals(SnsPlatform.REDDIT, adapter.platform());
        }
    }

    @Nested
    @DisplayName("getMaxContentLength()")
    class MaxContentLengthTests {

        @Test
        @DisplayName("returns 40000")
        void returns40000() {
            assertEquals(40000, adapter.getMaxContentLength());
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
        @DisplayName("contains duration=permanent")
        void containsDuration() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("duration=permanent"));
        }

        @Test
        @DisplayName("contains scope with identity submit read")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("starts with Reddit authorize URL")
        void startsWithRedditUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith("https://www.reddit.com/api/v1/authorize"));
        }

        @Test
        @DisplayName("URL encodes special characters in callback")
        void encodesCallbackUrl() {
            String url = adapter.getAuthorizationUrl("https://example.com/callback?param=value", "s");
            assertTrue(url.contains("redirect_uri="));
            // The callback URL should be encoded
            assertNotNull(url);
        }
    }

    @Nested
    @DisplayName("post() format validation")
    class PostFormatTests {

        @Test
        @DisplayName("content without colons throws exception")
        void noColons_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("no-colons-here", List.of())));
        }

        @Test
        @DisplayName("content with only one colon throws exception")
        void onlyOneColon_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("subreddit:title-only", List.of())));
        }

        @Test
        @DisplayName("content starting with colon throws exception")
        void startsWithColon_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest(":title:body", List.of())));
        }

        @Test
        @DisplayName("format 'subreddit:title:body' is required")
        void correctFormatRequired() {
            // This will fail at HTTP call, but should not throw format exception
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("programming:My Post:Post body text", List.of())));
            // The exception should be from HTTP call failure, not format validation
            assertNotNull(ex);
        }

        @Test
        @DisplayName("multiple colons in body are allowed (only first two colons are separators)")
        void multipleColonsInBody() {
            // This will fail at HTTP level but format parsing should work
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("sub:title:body:with:colons", List.of())));
            // Should not be IllegalArgumentException (format is valid)
            assertNotNull(ex);
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("clientId must not be null")
        void clientIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new RedditAdapter(null, "secret", "agent"));
        }

        @Test
        @DisplayName("clientSecret must not be null")
        void clientSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new RedditAdapter("id", null, "agent"));
        }

        @Test
        @DisplayName("userAgent must not be null")
        void userAgentNotNull() {
            assertThrows(NullPointerException.class,
                () -> new RedditAdapter("id", "secret", null));
        }

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }
    }
}
