package com.akazukin.domain.service;

import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostRepository;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = Objects.requireNonNull(postRepository, "postRepository must not be null");
    }

    public Post createPost(UUID userId, String content, List<String> mediaUrls, Instant scheduledAt) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post content must not be blank");
        }

        Instant now = Instant.now();
        PostStatus status = scheduledAt != null ? PostStatus.SCHEDULED : PostStatus.DRAFT;

        Post post = new Post(
                UUID.randomUUID(),
                userId,
                content,
                mediaUrls != null ? List.copyOf(mediaUrls) : List.of(),
                status,
                scheduledAt,
                now,
                now
        );

        return postRepository.save(post);
    }

    public Post getPost(UUID postId) {
        Objects.requireNonNull(postId, "postId must not be null");
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    public List<Post> getPostsByUser(UUID userId, int offset, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        return postRepository.findByUserId(userId, offset, limit);
    }

    public Post updatePost(UUID postId, String content, List<String> mediaUrls, Instant scheduledAt) {
        Post post = getPost(postId);

        if (!post.isEditable()) {
            throw new IllegalStateException("Post is not editable in status: " + post.getStatus());
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Post content must not be blank");
        }

        post.setContent(content);
        post.setMediaUrls(mediaUrls != null ? List.copyOf(mediaUrls) : List.of());
        post.setScheduledAt(scheduledAt);
        post.setStatus(scheduledAt != null ? PostStatus.SCHEDULED : PostStatus.DRAFT);
        post.setUpdatedAt(Instant.now());

        return postRepository.save(post);
    }

    public void deletePost(UUID postId) {
        Post post = getPost(postId);

        if (!post.isEditable()) {
            throw new IllegalStateException("Cannot delete post in status: " + post.getStatus());
        }

        postRepository.deleteById(postId);
    }

    public long countPostsByUser(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return postRepository.countByUserId(userId);
    }

    /**
     * Validates content length per platform.
     *
     * @param content  the post content to validate
     * @param platform the target SNS platform
     * @return a map of platform to error message; empty if valid
     */
    public Map<SnsPlatform, String> validateContent(String content, SnsPlatform platform) {
        Objects.requireNonNull(platform, "platform must not be null");

        Map<SnsPlatform, String> errors = new EnumMap<>(SnsPlatform.class);

        if (content == null || content.isEmpty()) {
            errors.put(platform, "Content must not be empty");
            return errors;
        }

        int maxLength = getMaxContentLength(platform);
        int contentLength = calculateContentLength(content);

        if (contentLength > maxLength) {
            errors.put(platform,
                    "Content exceeds maximum length for " + platform + ": "
                            + contentLength + " / " + maxLength + " characters");
        }

        return errors;
    }

    /**
     * Returns the character count of the content.
     *
     * @param content the content to measure
     * @return the character count, or 0 if content is null
     */
    public int calculateContentLength(String content) {
        if (content == null) {
            return 0;
        }
        return content.length();
    }

    private int getMaxContentLength(SnsPlatform platform) {
        return switch (platform) {
            case TWITTER -> 280;
            case BLUESKY -> 300;
            case MASTODON -> 500;
            case REDDIT -> 40000;
            case THREADS, INSTAGRAM, TELEGRAM, VK, PINTEREST -> 2200;
        };
    }
}
