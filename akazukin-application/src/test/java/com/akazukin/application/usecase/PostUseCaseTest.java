package com.akazukin.application.usecase;

import com.akazukin.application.dto.PostRequestDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private InMemorySnsAccountRepository snsAccountRepository;
    private NoOpPostPublisher postPublisher;
    private PostUseCase postUseCase;

    private UUID userId;
    private UUID twitterAccountId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        snsAccountRepository = new InMemorySnsAccountRepository();
        postPublisher = new NoOpPostPublisher();
        postUseCase = new PostUseCase(postRepository, postTargetRepository,
                snsAccountRepository, postPublisher);

        userId = UUID.randomUUID();
        twitterAccountId = UUID.randomUUID();

        Instant now = Instant.now();
        SnsAccount twitterAccount = new SnsAccount(
                twitterAccountId, userId, SnsPlatform.TWITTER, "@testuser", "Test User",
                "access_token", "refresh_token", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(twitterAccount);
    }

    @Test
    void createPost_createsPostWithPublishingStatusForImmediate() {
        PostRequestDto request = new PostRequestDto(
                "Hello world", List.of(), List.of("TWITTER"), null);

        PostResponseDto result = postUseCase.createPost(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Hello world", result.content());
        assertEquals("PUBLISHING", result.status());
        assertEquals(1, result.targets().size());
        assertEquals("TWITTER", result.targets().get(0).platform());
    }

    @Test
    void createPost_createsPostWithScheduledStatusForScheduled() {
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        PostRequestDto request = new PostRequestDto(
                "Scheduled post", List.of(), List.of("TWITTER"), scheduledAt);

        PostResponseDto result = postUseCase.createPost(userId, request);

        assertEquals("SCHEDULED", result.status());
        assertEquals(scheduledAt, result.scheduledAt());
    }

    @Test
    void createPost_throwsWhenContentIsBlank() {
        PostRequestDto request = new PostRequestDto(
                "", List.of(), List.of("TWITTER"), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPost_throwsWhenContentIsNull() {
        PostRequestDto request = new PostRequestDto(
                null, List.of(), List.of("TWITTER"), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPost_throwsWhenNoTargetPlatforms() {
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), List.of(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPost_throwsWhenTargetPlatformsIsNull() {
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), null, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPost_throwsWhenAccountNotConnectedForPlatform() {
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), List.of("BLUESKY"), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("ACCOUNT_NOT_CONNECTED", exception.getErrorCode());
    }

    @Test
    void createPost_throwsWhenScheduledTimeIsInPast() {
        Instant pastTime = Instant.now().minusSeconds(3600);
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), List.of("TWITTER"), pastTime);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.createPost(userId, request));
        assertEquals("INVALID_SCHEDULE", exception.getErrorCode());
    }

    @Test
    void getPost_returnsExistingPost() {
        PostRequestDto request = new PostRequestDto(
                "Hello world", List.of(), List.of("TWITTER"), null);
        PostResponseDto created = postUseCase.createPost(userId, request);

        PostResponseDto found = postUseCase.getPost(created.id());

        assertEquals(created.id(), found.id());
        assertEquals("Hello world", found.content());
    }

    @Test
    void getPost_throwsPostNotFoundExceptionForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postUseCase.getPost(nonExistentId));
    }

    @Test
    void listPosts_returnsPaginatedResults() {
        PostRequestDto request1 = new PostRequestDto(
                "Post 1", List.of(), List.of("TWITTER"), null);
        PostRequestDto request2 = new PostRequestDto(
                "Post 2", List.of(), List.of("TWITTER"), null);
        PostRequestDto request3 = new PostRequestDto(
                "Post 3", List.of(), List.of("TWITTER"), null);

        postUseCase.createPost(userId, request1);
        postUseCase.createPost(userId, request2);
        postUseCase.createPost(userId, request3);

        List<PostResponseDto> result = postUseCase.listPosts(userId, 0, 2);

        assertEquals(2, result.size());
    }

    @Test
    void listPosts_returnsEmptyForUserWithNoPosts() {
        UUID otherUserId = UUID.randomUUID();

        List<PostResponseDto> result = postUseCase.listPosts(otherUserId, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void deletePost_removesPostAndTargets() {
        // Use a scheduled post so status is SCHEDULED (editable)
        Instant future = Instant.now().plusSeconds(3600);
        PostRequestDto request = new PostRequestDto(
                "To delete", List.of(), List.of("TWITTER"), future);
        PostResponseDto created = postUseCase.createPost(userId, request);

        postUseCase.deletePost(created.id(), userId);

        assertThrows(PostNotFoundException.class,
                () -> postUseCase.getPost(created.id()));
    }

    @Test
    void deletePost_throwsForbiddenWhenNotOwner() {
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), List.of("TWITTER"), null);
        PostResponseDto created = postUseCase.createPost(userId, request);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.deletePost(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void deletePost_throwsPostNotEditableWhenAlreadyPublished() {
        PostRequestDto request = new PostRequestDto(
                "Hello", List.of(), List.of("TWITTER"), null);
        PostResponseDto created = postUseCase.createPost(userId, request);

        Post post = postRepository.findById(created.id()).orElseThrow();
        post.setStatus(PostStatus.PUBLISHED);
        postRepository.save(post);

        DomainException exception = assertThrows(DomainException.class,
                () -> postUseCase.deletePost(created.id(), userId));
        assertEquals("POST_NOT_EDITABLE", exception.getErrorCode());
    }

    @Test
    void deletePost_throwsPostNotFoundForNonExistent() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postUseCase.deletePost(nonExistentId, userId));
    }

    @Test
    void deletePost_cancelsScheduledPostBeforeDeletion() {
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        PostRequestDto request = new PostRequestDto(
                "Scheduled", List.of(), List.of("TWITTER"), scheduledAt);
        PostResponseDto created = postUseCase.createPost(userId, request);

        postUseCase.deletePost(created.id(), userId);

        assertTrue(postPublisher.cancelledPostIds.contains(created.id()));
    }

    // --- In-memory implementations ---

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
            return new ArrayList<>(userPosts.subList(offset, end));
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

        @Override
        public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
            return new java.util.EnumMap<>(SnsPlatform.class);
        }
    }

    private static class InMemoryPostTargetRepository implements PostTargetRepository {

        private final Map<UUID, PostTarget> store = new HashMap<>();

        @Override
        public Optional<PostTarget> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<PostTarget> findByPostId(UUID postId) {
            return store.values().stream()
                    .filter(target -> target.getPostId().equals(postId))
                    .toList();
        }

        @Override
        public PostTarget save(PostTarget target) {
            store.put(target.getId(), target);
            return target;
        }

        @Override
        public void deleteByPostId(UUID postId) {
            List<UUID> toRemove = store.values().stream()
                    .filter(target -> target.getPostId().equals(postId))
                    .map(PostTarget::getId)
                    .toList();
            toRemove.forEach(store::remove);
        }

        @Override
        public List<PostTarget> findByPostIds(List<UUID> postIds) {
            if (postIds == null || postIds.isEmpty()) {
                return List.of();
            }
            return store.values().stream()
                    .filter(target -> postIds.contains(target.getPostId()))
                    .toList();
        }

        @Override
        public void updateStatus(UUID id, PostStatus status, String errorMessage) {
            PostTarget target = store.get(id);
            if (target != null) {
                target.setStatus(status);
                target.setErrorMessage(errorMessage);
            }
        }
    }

    private static class InMemorySnsAccountRepository implements SnsAccountRepository {

        private final Map<UUID, SnsAccount> store = new HashMap<>();

        @Override
        public Optional<SnsAccount> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<SnsAccount> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(account -> account.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
            return store.values().stream()
                    .filter(account -> account.getUserId().equals(userId))
                    .filter(account -> account.getPlatform() == platform)
                    .findFirst();
        }

        @Override
        public SnsAccount save(SnsAccount snsAccount) {
            store.put(snsAccount.getId(), snsAccount);
            return snsAccount;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public List<SnsAccount> findAllByIds(Collection<UUID> ids) {
            return store.values().stream()
                    .filter(account -> ids.contains(account.getId()))
                    .toList();
        }

        @Override
        public long countByPlatform(SnsPlatform platform) {
            return store.values().stream()
                    .filter(account -> account.getPlatform() == platform)
                    .count();
        }

        @Override
        public long countAll() {
            return store.size();
        }
    }

    private static class NoOpPostPublisher implements PostPublisher {

        final List<UUID> publishedPostIds = new ArrayList<>();
        final List<UUID> scheduledPostIds = new ArrayList<>();
        final List<UUID> cancelledPostIds = new ArrayList<>();

        @Override
        public void publishForProcessing(UUID postId) {
            publishedPostIds.add(postId);
        }

        @Override
        public void schedulePost(UUID postId, Instant scheduledAt) {
            scheduledPostIds.add(postId);
        }

        @Override
        public void cancelScheduledPost(UUID postId) {
            cancelledPostIds.add(postId);
        }
    }
}
