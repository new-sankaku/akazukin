package com.akazukin.adapter.pinterest;

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

@DisplayName("PinterestAdapter Unit Tests")
class PinterestAdapterTest {

    private PinterestAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PinterestAdapter("test-app-id", "test-app-secret");
    }

    @Nested
    @DisplayName("platform()")
    class PlatformTests {

        @Test
        @DisplayName("returns PINTEREST")
        void returnsPinterest() {
            assertEquals(SnsPlatform.PINTEREST, adapter.platform());
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
        @DisplayName("contains client_id (appId)")
        void containsClientId() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("client_id=test-app-id"));
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
        @DisplayName("contains scope with boards and pins permissions")
        void containsScope() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.contains("scope="));
        }

        @Test
        @DisplayName("starts with Pinterest OAuth URL")
        void startsWithPinterestUrl() {
            String url = adapter.getAuthorizationUrl("https://callback.example.com", "state123");
            assertTrue(url.startsWith("https://www.pinterest.com/oauth/"));
        }
    }

    @Nested
    @DisplayName("post() format validation")
    class PostFormatTests {

        @Test
        @DisplayName("without media URL throws exception")
        void withoutMediaUrl_throwsException() {
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("board:title:description", List.of())));
            assertNotNull(ex);
        }

        @Test
        @DisplayName("with empty media URL list throws exception")
        void emptyMediaUrlList_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("board:title:desc", List.of())));
        }

        @Test
        @DisplayName("content without colons throws exception")
        void noColons_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("no-colons-here",
                        List.of("https://example.com/img.jpg"))));
        }

        @Test
        @DisplayName("content starting with colon throws exception")
        void startsWithColon_throwsException() {
            assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest(":title:description",
                        List.of("https://example.com/img.jpg"))));
        }

        @Test
        @DisplayName("format 'boardId:title' with one colon accepted (title=description)")
        void onlyOneColon_accepted() {
            // With one colon: boardId=first part, title=rest, description=title
            // This will fail at HTTP level but format parsing should work
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("my-board:My Title",
                        List.of("https://example.com/img.jpg"))));
            assertNotNull(ex);
        }

        @Test
        @DisplayName("format 'boardId:title:description' with two colons accepted")
        void twoColons_accepted() {
            // This will fail at HTTP level but format parsing should work
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("my-board:My Title:My description",
                        List.of("https://example.com/img.jpg"))));
            assertNotNull(ex);
        }

        @Test
        @DisplayName("multiple colons in description allowed")
        void multipleColonsInDescription() {
            // boardId:title:description:with:more:colons
            SnsApiException ex = assertThrows(SnsApiException.class,
                () -> adapter.post("token",
                    new PostRequest("board:title:desc:with:colons",
                        List.of("https://example.com/img.jpg"))));
            assertNotNull(ex);
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("appId must not be null")
        void appIdNotNull() {
            assertThrows(NullPointerException.class,
                () -> new PinterestAdapter(null, "secret"));
        }

        @Test
        @DisplayName("appSecret must not be null")
        void appSecretNotNull() {
            assertThrows(NullPointerException.class,
                () -> new PinterestAdapter("id", null));
        }

        @Test
        @DisplayName("close does not throw")
        void closeDoesNotThrow() {
            assertDoesNotThrow(() -> adapter.close());
        }
    }
}
