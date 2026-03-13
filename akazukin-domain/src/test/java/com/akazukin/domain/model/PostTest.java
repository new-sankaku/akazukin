package com.akazukin.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostTest {

    private Post createPost(PostStatus status, Instant scheduledAt) {
        Instant now = Instant.now();
        return new Post(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test content",
                List.of(),
                status,
                scheduledAt,
                now,
                now
        );
    }

    @Test
    void isScheduled_returnsTrueWhenScheduledAtIsSet() {
        Post post = createPost(PostStatus.SCHEDULED, Instant.now().plusSeconds(3600));

        assertTrue(post.isScheduled());
    }

    @Test
    void isScheduled_returnsFalseWhenScheduledAtIsNull() {
        Post post = createPost(PostStatus.DRAFT, null);

        assertFalse(post.isScheduled());
    }

    @Test
    void isEditable_returnsTrueForDraft() {
        Post post = createPost(PostStatus.DRAFT, null);

        assertTrue(post.isEditable());
    }

    @Test
    void isEditable_returnsTrueForScheduled() {
        Post post = createPost(PostStatus.SCHEDULED, Instant.now().plusSeconds(3600));

        assertTrue(post.isEditable());
    }

    @Test
    void isEditable_returnsFalseForPublishing() {
        Post post = createPost(PostStatus.PUBLISHING, null);

        assertFalse(post.isEditable());
    }

    @Test
    void isEditable_returnsFalseForPublished() {
        Post post = createPost(PostStatus.PUBLISHED, null);

        assertFalse(post.isEditable());
    }

    @Test
    void isEditable_returnsFalseForFailed() {
        Post post = createPost(PostStatus.FAILED, null);

        assertFalse(post.isEditable());
    }

    @Test
    void equals_returnsTrueForSameId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Post post1 = new Post(id, UUID.randomUUID(), "Content 1", List.of(),
                PostStatus.DRAFT, null, now, now);
        Post post2 = new Post(id, UUID.randomUUID(), "Content 2", List.of(),
                PostStatus.PUBLISHED, null, now, now);

        assertEquals(post1, post2);
    }

    @Test
    void equals_returnsFalseForDifferentId() {
        Instant now = Instant.now();

        Post post1 = new Post(UUID.randomUUID(), UUID.randomUUID(), "Content", List.of(),
                PostStatus.DRAFT, null, now, now);
        Post post2 = new Post(UUID.randomUUID(), UUID.randomUUID(), "Content", List.of(),
                PostStatus.DRAFT, null, now, now);

        assertNotEquals(post1, post2);
    }

    @Test
    void hashCode_sameForSameId() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Post post1 = new Post(id, UUID.randomUUID(), "Content 1", List.of(),
                PostStatus.DRAFT, null, now, now);
        Post post2 = new Post(id, UUID.randomUUID(), "Content 2", List.of(),
                PostStatus.PUBLISHED, null, now, now);

        assertEquals(post1.hashCode(), post2.hashCode());
    }

    @Test
    void equals_returnsFalseForNull() {
        Post post = createPost(PostStatus.DRAFT, null);

        assertNotEquals(null, post);
    }

    @Test
    void equals_returnsTrueForSameInstance() {
        Post post = createPost(PostStatus.DRAFT, null);

        assertEquals(post, post);
    }
}
