package com.akazukin.adapter.instagram;

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

@DisplayName("InstagramAdapter Unit Tests")
class InstagramAdapterTest {

    private InstagramAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InstagramAdapter("test-client-id", "test-client-secret");
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns INSTAGRAM")
        void returnsInstagram() {
            assertEquals(SnsPlatform.INSTAGRAM, adapter.platform());
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
        @DisplayName("contains scope with instagram permissions")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("starts with Facebook Graph API OAuth URL")
        void startsWithFacebookUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith("https://www.facebook.com/"));
        }

        @Test
        @DisplayName("contains dialog/oauth path")
        void containsOAuthPath() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("/dialog/oauth"));
        }
    }

    @Nested
    @DisplayName("deletePost()")
    class DeletePostTests {

        @Test
        @DisplayName("throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> adapter.deletePost("token", "post-id"));
            assertTrue(ex.getMessage().contains("Instagram"));
        }

        @Test
        @DisplayName("exception message mentions Graph API limitation")
        void exceptionMentionsGraphApi() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> adapter.deletePost("token", "post-id"));
            assertTrue(ex.getMessage().contains("Graph API"));
        }

        @Test
        @DisplayName("exception message mentions deleting through app")
        void exceptionMentionsApp() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> adapter.deletePost("any-token", "any-id"));
            assertTrue(ex.getMessage().contains("app") || ex.getMessage().contains("website"));
        }
    }

    @Nested
    @DisplayName("post() validation")
    class PostValidationTests {

        @Test
        @DisplayName("without media URL throws exception")
        void withoutMediaUrl_throwsException() {
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("Hello Instagram!", List.of())));
            assertNotNull(ex);
        }

        @Test
        @DisplayName("with empty media URL list throws exception")
        void emptyMediaUrlList_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token", new PostRequest("Caption", List.of())));
        }

        @Test
        @DisplayName("with media URL attempts post (fails at HTTP level)")
        void withMediaUrl_attemptsPost() {
            // This will fail at HTTP call level since we're hitting real Graph API
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("Caption", List.of("https://example.com/img.jpg"))));
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
                () -> new InstagramAdapter(null, "secret"));
        }

        @Test
        @DisplayName("clientSecret must not be null")
        void clientSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new InstagramAdapter("id", null));
        }

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }
    }
}
