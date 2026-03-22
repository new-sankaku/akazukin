package com.akazukin.application.usecase;

import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.CircuitBreakerState;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.domain.port.CircuitBreakerRegistry;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAdapter;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulePostUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private NoOpPostPublisher postPublisher;
    private SchedulePostUseCase schedulePostUseCase;

    private UUID userId;
    private UUID twitterAccountId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        postPublisher = new NoOpPostPublisher();

        InMemorySnsAccountRepository snsAccountRepository = new InMemorySnsAccountRepository();
        StubSnsAdapterLookup snsAdapterLookup = new StubSnsAdapterLookup();
        StubCircuitBreakerRegistry circuitBreakerRegistry = new StubCircuitBreakerRegistry();

        userId = UUID.randomUUID();
        twitterAccountId = UUID.randomUUID();

        Instant now = Instant.now();
        SnsAccount twitterAccount = new SnsAccount(
                twitterAccountId, userId, SnsPlatform.TWITTER, "@testuser", "Test User",
                "access_token", "refresh_token", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(twitterAccount);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter());

        PostPublishUseCase postPublishUseCase = new PostPublishUseCase(
                postRepository, postTargetRepository, snsAccountRepository,
                snsAdapterLookup, circuitBreakerRegistry);

        schedulePostUseCase = new SchedulePostUseCase(
                postRepository, postTargetRepository, postPublisher, postPublishUseCase);
    }

    @Test
    void processScheduledPost_delegatesToPostPublishUseCase() {
        Post post = createScheduledPost();
        createTarget(post.getId());

        schedulePostUseCase.processScheduledPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    }

    @Test
    void processScheduledPost_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> schedulePostUseCase.processScheduledPost(nonExistentId));
    }

    @Test
    void findPostsDueForPublishing_returnsDuePosts() {
        Post duePast = createPostWithSchedule(PostStatus.SCHEDULED, Instant.now().minusSeconds(60));
        Post dueFuture = createPostWithSchedule(PostStatus.SCHEDULED, Instant.now().plusSeconds(3600));

        List<Post> due = schedulePostUseCase.findPostsDueForPublishing();

        assertEquals(1, due.size());
        assertEquals(duePast.getId(), due.get(0).getId());
    }

    @Test
    void findPostsDueForPublishing_returnsEmptyWhenNoDuePosts() {
        createPostWithSchedule(PostStatus.SCHEDULED, Instant.now().plusSeconds(3600));

        List<Post> due = schedulePostUseCase.findPostsDueForPublishing();

        assertTrue(due.isEmpty());
    }

    @Test
    void cancelScheduledPost_movesPostToDraftStatus() {
        Post post = createScheduledPost();
        PostTarget target = createTarget(post.getId());

        schedulePostUseCase.cancelScheduledPost(post.getId(), userId);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.DRAFT, updated.getStatus());
        assertNull(updated.getScheduledAt());
    }

    @Test
    void cancelScheduledPost_cancelsViaPostPublisher() {
        Post post = createScheduledPost();

        schedulePostUseCase.cancelScheduledPost(post.getId(), userId);

        assertTrue(postPublisher.cancelledPostIds.contains(post.getId()));
    }

    @Test
    void cancelScheduledPost_movesTargetsToDraft() {
        Post post = createScheduledPost();
        PostTarget target = createTarget(post.getId());

        schedulePostUseCase.cancelScheduledPost(post.getId(), userId);

        List<PostTarget> targets = postTargetRepository.findByPostId(post.getId());
        for (PostTarget t : targets) {
            assertEquals(PostStatus.DRAFT, t.getStatus());
        }
    }

    @Test
    void cancelScheduledPost_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> schedulePostUseCase.cancelScheduledPost(nonExistentId, userId));
    }

    @Test
    void cancelScheduledPost_throwsForbiddenWhenNotOwner() {
        Post post = createScheduledPost();
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.cancelScheduledPost(post.getId(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void cancelScheduledPost_throwsPostNotScheduledWhenDraft() {
        Post post = createPostWithSchedule(PostStatus.DRAFT, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.cancelScheduledPost(post.getId(), userId));
        assertEquals("POST_NOT_SCHEDULED", exception.getErrorCode());
    }

    @Test
    void cancelScheduledPost_throwsPostNotScheduledWhenPublished() {
        Post post = createPostWithSchedule(PostStatus.PUBLISHED, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.cancelScheduledPost(post.getId(), userId));
        assertEquals("POST_NOT_SCHEDULED", exception.getErrorCode());
    }

    @Test
    void reschedulePost_updatesScheduledTime() {
        Post post = createScheduledPost();
        Instant newTime = Instant.now().plusSeconds(7200);

        schedulePostUseCase.reschedulePost(post.getId(), userId, newTime);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(newTime, updated.getScheduledAt());
    }

    @Test
    void reschedulePost_cancelsOldScheduleAndCreatesNew() {
        Post post = createScheduledPost();
        Instant newTime = Instant.now().plusSeconds(7200);

        schedulePostUseCase.reschedulePost(post.getId(), userId, newTime);

        assertTrue(postPublisher.cancelledPostIds.contains(post.getId()));
        assertTrue(postPublisher.scheduledPostIds.contains(post.getId()));
    }

    @Test
    void reschedulePost_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();
        Instant newTime = Instant.now().plusSeconds(7200);

        assertThrows(PostNotFoundException.class,
                () -> schedulePostUseCase.reschedulePost(nonExistentId, userId, newTime));
    }

    @Test
    void reschedulePost_throwsForbiddenWhenNotOwner() {
        Post post = createScheduledPost();
        UUID otherUserId = UUID.randomUUID();
        Instant newTime = Instant.now().plusSeconds(7200);

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.reschedulePost(post.getId(), otherUserId, newTime));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void reschedulePost_throwsPostNotScheduledWhenDraft() {
        Post post = createPostWithSchedule(PostStatus.DRAFT, null);
        Instant newTime = Instant.now().plusSeconds(7200);

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.reschedulePost(post.getId(), userId, newTime));
        assertEquals("POST_NOT_SCHEDULED", exception.getErrorCode());
    }

    @Test
    void reschedulePost_throwsInvalidScheduleWhenTimeIsInPast() {
        Post post = createScheduledPost();
        Instant pastTime = Instant.now().minusSeconds(3600);

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.reschedulePost(post.getId(), userId, pastTime));
        assertEquals("INVALID_SCHEDULE", exception.getErrorCode());
    }

    @Test
    void reschedulePost_throwsInvalidScheduleWhenTimeIsNull() {
        Post post = createScheduledPost();

        DomainException exception = assertThrows(DomainException.class,
                () -> schedulePostUseCase.reschedulePost(post.getId(), userId, null));
        assertEquals("INVALID_SCHEDULE", exception.getErrorCode());
    }

    private Post createScheduledPost() {
        return createPostWithSchedule(PostStatus.SCHEDULED, Instant.now().plusSeconds(3600));
    }

    private Post createPostWithSchedule(PostStatus status, Instant scheduledAt) {
        Instant now = Instant.now();
        Post post = new Post(UUID.randomUUID(), userId, "Test content", List.of(),
                status, scheduledAt, now, now);
        return postRepository.save(post);
    }

    private PostTarget createTarget(UUID postId) {
        Instant now = Instant.now();
        PostTarget target = new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, null, PostStatus.SCHEDULED, null, null, now);
        return postTargetRepository.save(target);
    }

    private static class InMemoryPostRepository implements PostRepository {

        private final Map<UUID, Post> store = new HashMap<>();

        @Override
        public Optional<Post> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Post> findByUserId(UUID userId, int offset, int limit) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public List<Post> findScheduledBefore(Instant before) {
            return store.values().stream()
                    .filter(p -> p.getStatus() == PostStatus.SCHEDULED)
                    .filter(p -> p.getScheduledAt() != null && p.getScheduledAt().isBefore(before))
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
            return store.values().stream().filter(p -> p.getUserId().equals(userId)).count();
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, PostStatus status) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId) && p.getStatus() == status).count();
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
                    .filter(t -> t.getPostId().equals(postId)).toList();
        }

        @Override
        public PostTarget save(PostTarget target) {
            store.put(target.getId(), target);
            return target;
        }

        @Override
        public void deleteByPostId(UUID postId) {
            List<UUID> toRemove = store.values().stream()
                    .filter(t -> t.getPostId().equals(postId))
                    .map(PostTarget::getId).toList();
            toRemove.forEach(store::remove);
        }

        @Override
        public List<PostTarget> findByPostIds(List<UUID> postIds) {
            if (postIds == null || postIds.isEmpty()) return List.of();
            return store.values().stream()
                    .filter(t -> postIds.contains(t.getPostId())).toList();
        }

        @Override
        public void updateStatus(UUID id, PostStatus status, String errorMessage) {
            PostTarget target = store.get(id);
            if (target != null) {
                target.setStatus(status);
                target.setErrorMessage(errorMessage);
            }
        }

        @Override
        public Map<String, Long> countByStatusForUser(UUID userId) {
            return Map.of();
        }

        @Override
        public List<PostTarget> findByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<PostTarget> findByUserIdAndPlatformAndCreatedAtBetween(UUID userId, SnsPlatform platform, Instant from, Instant to) {
            return List.of();
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
                    .filter(a -> a.getUserId().equals(userId)).toList();
        }

        @Override
        public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
            return store.values().stream()
                    .filter(a -> a.getUserId().equals(userId) && a.getPlatform() == platform)
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
                    .filter(a -> ids.contains(a.getId())).toList();
        }

        @Override
        public long countByPlatform(SnsPlatform platform) {
            return store.values().stream().filter(a -> a.getPlatform() == platform).count();
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

    private static class StubSnsAdapterLookup implements SnsAdapterLookup {

        private final Map<SnsPlatform, SnsAdapter> adapters = new HashMap<>();

        void registerAdapter(SnsPlatform platform, SnsAdapter adapter) {
            adapters.put(platform, adapter);
        }

        @Override
        public SnsAdapter getAdapter(SnsPlatform platform) {
            SnsAdapter adapter = adapters.get(platform);
            if (adapter == null) {
                throw new IllegalArgumentException("No adapter for " + platform);
            }
            return adapter;
        }
    }

    private static class StubCircuitBreakerRegistry implements CircuitBreakerRegistry {

        @Override
        public CircuitBreakerState getState(SnsPlatform platform) {
            return null;
        }

        @Override
        public List<CircuitBreakerState> getAllStates() {
            return List.of();
        }

        @Override
        public void recordSuccess(SnsPlatform platform) {
        }

        @Override
        public void recordFailure(SnsPlatform platform) {
        }

        @Override
        public boolean isCallPermitted(SnsPlatform platform) {
            return true;
        }
    }

    private static class SuccessSnsAdapter implements SnsAdapter {

        @Override
        public SnsPlatform platform() {
            return SnsPlatform.TWITTER;
        }

        @Override
        public String getAuthorizationUrl(String callbackUrl, String state) {
            return "";
        }

        @Override
        public SnsAuthToken exchangeToken(String code, String callbackUrl) {
            return null;
        }

        @Override
        public SnsAuthToken refreshToken(String refreshToken) {
            return null;
        }

        @Override
        public SnsProfile getProfile(String accessToken) {
            return null;
        }

        @Override
        public PostResult post(String accessToken, PostRequest request) {
            return new PostResult("platform-post-id", "https://example.com/post", Instant.now());
        }

        @Override
        public void deletePost(String accessToken, String postId) {
        }

        @Override
        public int getMaxContentLength() {
            return 280;
        }
    }
}
