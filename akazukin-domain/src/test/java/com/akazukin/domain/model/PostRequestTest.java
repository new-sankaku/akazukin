package com.akazukin.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostRequestTest {

    @Test
    void constructor_throwsWhenContentIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PostRequest(null, List.of())
        );

        assertTrue(exception.getMessage().contains("blank"));
    }

    @Test
    void constructor_throwsWhenContentIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new PostRequest("   ", List.of())
        );

        assertTrue(exception.getMessage().contains("blank"));
    }

    @Test
    void constructor_throwsWhenContentIsEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PostRequest("", List.of())
        );
    }

    @Test
    void mediaUrls_defaultsToEmptyListWhenNull() {
        PostRequest request = new PostRequest("Hello world", null);

        assertTrue(request.mediaUrls().isEmpty());
    }

    @Test
    void mediaUrls_isImmutableCopy() {
        List<String> urls = new ArrayList<>();
        urls.add("https://example.com/image.png");

        PostRequest request = new PostRequest("Hello world", urls);

        urls.add("https://example.com/image2.png");

        assertEquals(1, request.mediaUrls().size());
    }

    @Test
    void mediaUrls_cannotBeModifiedAfterCreation() {
        PostRequest request = new PostRequest("Hello world", List.of("https://example.com/image.png"));

        assertThrows(
                UnsupportedOperationException.class,
                () -> request.mediaUrls().add("https://example.com/new.png")
        );
    }

    @Test
    void constructor_createsValidRequest() {
        PostRequest request = new PostRequest("Test content", List.of("https://example.com/img.png"));

        assertEquals("Test content", request.content());
        assertEquals(1, request.mediaUrls().size());
        assertEquals("https://example.com/img.png", request.mediaUrls().get(0));
    }
}
