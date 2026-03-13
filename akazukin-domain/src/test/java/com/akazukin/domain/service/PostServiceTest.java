package com.akazukin.domain.service;

import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.port.PostRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostServiceTest {

    private InMemoryPostRepository postRepository;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postService = new PostService(postRepository);
    }

    @Test
    void constructor_throwsWhenRepositoryIsNull() {
        assertThrows(NullPointerException.class, () -> new PostService(null));
    }

    @Test
    void createPost_createsPostWithDraftStatusWhenNotScheduled() {
        UUID userId = UUID.randomUUID();

        Post result = postService.createPost(userId, "Hello world", List.of(), null);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals("Hello world", result.getContent());
        assertEquals(PostStatus.DRAFT, result.getStatus());
        assertTrue(result.getMediaUrls().isEmpty());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createPost_createsPostWithScheduledStatusWhenScheduledAtSet() {
        UUID userId = UUID.randomUUID();
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        Post result = postService.createPost(userId, "Scheduled post", List.of(), scheduledAt);

        assertEquals(PostStatus.SCHEDULED, result.getStatus());
        assertEquals(scheduledAt, result.getScheduledAt());
    }

    @Test
    void createPost_throwsWhenUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> postService.createPost(null, "Content", List.of(), null));
    }

    @Test
    void createPost_throwsWhenContentIsNull() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> postService.createPost(userId, null, List.of(), null));
    }

    @Test
    void createPost_throwsWhenContentIsBlank() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> postService.createPost(userId, "   ", List.of(), null));
    }

    @Test
    void createPost_handlesNullMediaUrls() {
        UUID userId = UUID.randomUUID();

        Post result = postService.createPost(userId, "Content", null, null);

        assertTrue(result.getMediaUrls().isEmpty());
    }

    @Test
    void createPost_copiesMediaUrls() {
        UUID userId = UUID.randomUUID();
        List<String> urls = new ArrayList<>();
        urls.add("https://example.com/img.png");

        Post result = postService.createPost(userId, "Content", urls, null);

        assertEquals(1, result.getMediaUrls().size());
    }

    @Test
    void getPost_returnsExistingPost() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Hello", List.of(), null);

        Post found = postService.getPost(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Hello", found.getContent());
    }

    @Test
    void getPost_throwsPostNotFoundExceptionForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postService.getPost(nonExistentId));
    }

    @Test
    void getPost_throwsWhenPostIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> postService.getPost(null));
    }

    @Test
    void getPostsByUser_returnsPaginatedResults() {
        UUID userId = UUID.randomUUID();
        postService.createPost(userId, "Post 1", List.of(), null);
        postService.createPost(userId, "Post 2", List.of(), null);
        postService.createPost(userId, "Post 3", List.of(), null);

        List<Post> result = postService.getPostsByUser(userId, 0, 2);

        assertEquals(2, result.size());
    }

    @Test
    void getPostsByUser_returnsEmptyForNoPostsUser() {
        UUID userId = UUID.randomUUID();

        List<Post> result = postService.getPostsByUser(userId, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getPostsByUser_throwsWhenUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> postService.getPostsByUser(null, 0, 10));
    }

    @Test
    void updatePost_updatesContentAndMediaUrls() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Original", List.of(), null);

        Post updated = postService.updatePost(created.getId(), "Updated content",
                List.of("https://example.com/new.png"), null);

        assertEquals("Updated content", updated.getContent());
        assertEquals(1, updated.getMediaUrls().size());
        assertEquals(PostStatus.DRAFT, updated.getStatus());
    }

    @Test
    void updatePost_changesStatusToScheduledWhenScheduledAtSet() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Original", List.of(), null);
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        Post updated = postService.updatePost(created.getId(), "Updated", List.of(), scheduledAt);

        assertEquals(PostStatus.SCHEDULED, updated.getStatus());
        assertEquals(scheduledAt, updated.getScheduledAt());
    }

    @Test
    void updatePost_throwsWhenPostIsNotEditable() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Original", List.of(), null);
        created.setStatus(PostStatus.PUBLISHED);
        postRepository.save(created);

        assertThrows(IllegalStateException.class,
                () -> postService.updatePost(created.getId(), "Updated", List.of(), null));
    }

    @Test
    void updatePost_throwsWhenContentIsBlank() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Original", List.of(), null);

        assertThrows(IllegalArgumentException.class,
                () -> postService.updatePost(created.getId(), "", List.of(), null));
    }

    @Test
    void updatePost_throwsWhenPostNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postService.updatePost(nonExistentId, "Content", List.of(), null));
    }

    @Test
    void deletePost_removesPost() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "To delete", List.of(), null);

        postService.deletePost(created.getId());

        assertThrows(PostNotFoundException.class,
                () -> postService.getPost(created.getId()));
    }

    @Test
    void deletePost_throwsWhenPostIsNotEditable() {
        UUID userId = UUID.randomUUID();
        Post created = postService.createPost(userId, "Published", List.of(), null);
        created.setStatus(PostStatus.PUBLISHING);
        postRepository.save(created);

        assertThrows(IllegalStateException.class,
                () -> postService.deletePost(created.getId()));
    }

    @Test
    void deletePost_throwsWhenPostNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postService.deletePost(nonExistentId));
    }

    @Test
    void countPostsByUser_returnsCorrectCount() {
        UUID userId = UUID.randomUUID();
        postService.createPost(userId, "Post 1", List.of(), null);
        postService.createPost(userId, "Post 2", List.of(), null);

        long count = postService.countPostsByUser(userId);

        assertEquals(2, count);
    }

    @Test
    void countPostsByUser_returnsZeroForNoPostsUser() {
        UUID userId = UUID.randomUUID();

        long count = postService.countPostsByUser(userId);

        assertEquals(0, count);
    }

    @Test
    void countPostsByUser_throwsWhenUserIdIsNull() {
        assertThrows(NullPointerException.class,
                () -> postService.countPostsByUser(null));
    }

    /**
     * Simple in-memory PostRepository for testing.
     */
    private static class InMemoryPostRepository implements PostRepository {

        private final Map<UUID, Post> store = new HashMap<>();

        @Override
        public Optional<Post> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Post> findByUserId(UUID userId, int offset, int limit) {
            List<Post> userPosts = store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .toList();

            int end = Math.min(offset + limit, userPosts.size());
            if (offset >= userPosts.size()) {
                return List.of();
            }
            return userPosts.subList(offset, end);
        }

        @Override
        public List<Post> findScheduledBefore(Instant before) {
            return store.values().stream()
                    .filter(post -> post.getStatus() == PostStatus.SCHEDULED)
                    .filter(post -> post.getScheduledAt() != null && post.getScheduledAt().isBefore(before))
                    .toList();
        }

        @Override
        public Post save(Post post) {
            store.put(post.getId(), post);
            return post;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .count();
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, PostStatus status) {
            return store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .filter(post -> post.getStatus() == status)
                    .count();
        }
    }
}
