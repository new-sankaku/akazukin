package com.akazukin.application.usecase;

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
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostPublishUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private InMemorySnsAccountRepository snsAccountRepository;
    private StubSnsAdapterLookup snsAdapterLookup;
    private StubCircuitBreakerRegistry circuitBreakerRegistry;
    private PostPublishUseCase postPublishUseCase;

    private UUID userId;
    private UUID twitterAccountId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        snsAccountRepository = new InMemorySnsAccountRepository();
        snsAdapterLookup = new StubSnsAdapterLookup();
        circuitBreakerRegistry = new StubCircuitBreakerRegistry();
        postPublishUseCase = new PostPublishUseCase(postRepository, postTargetRepository,
                snsAccountRepository, snsAdapterLookup, circuitBreakerRegistry);

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
    void processPost_publishesSingleTargetSuccessfully() {
        Post post = createPost(PostStatus.PUBLISHING);
        PostTarget target = createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    }

    @Test
    void processPost_marksPostAsFailedWhenAllTargetsFail() {
        Post post = createPost(PostStatus.PUBLISHING);
        PostTarget target = createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new FailingSnsAdapter(SnsPlatform.TWITTER));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.FAILED, updated.getStatus());
    }

    @Test
    void processPost_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postPublishUseCase.processPost(nonExistentId));
    }

    @Test
    void processPost_skipsPostInDraftStatus() {
        Post post = createPost(PostStatus.DRAFT);

        postPublishUseCase.processPost(post.getId());

        Post unchanged = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.DRAFT, unchanged.getStatus());
    }

    @Test
    void processPost_skipsPostInPublishedStatus() {
        Post post = createPost(PostStatus.PUBLISHED);

        postPublishUseCase.processPost(post.getId());

        Post unchanged = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, unchanged.getStatus());
    }

    @Test
    void processPost_marksPostAsFailedWhenNoTargetsExist() {
        Post post = createPost(PostStatus.PUBLISHING);

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.FAILED, updated.getStatus());
    }

    @Test
    void processPost_processesScheduledStatusPost() {
        Post post = createPost(PostStatus.SCHEDULED);
        PostTarget target = createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    }

    @Test
    void processPost_marksTargetAsFailedWhenAccountNotFound() {
        Post post = createPost(PostStatus.PUBLISHING);
        UUID missingAccountId = UUID.randomUUID();
        PostTarget target = createTarget(post.getId(), missingAccountId, SnsPlatform.TWITTER);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.FAILED, updated.getStatus());
    }

    @Test
    void processPost_failsWhenCircuitBreakerIsOpen() {
        Post post = createPost(PostStatus.PUBLISHING);
        PostTarget target = createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));
        circuitBreakerRegistry.setCallPermitted(false);

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.FAILED, updated.getStatus());
    }

    @Test
    void processPost_publishesMultipleTargetsAndMarksPublishedWhenAllSucceed() {
        UUID blueskyAccountId = UUID.randomUUID();
        Instant now = Instant.now();
        SnsAccount blueskyAccount = new SnsAccount(
                blueskyAccountId, userId, SnsPlatform.BLUESKY, "@testuser", "Test User",
                "access_token_bs", "refresh_token_bs", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(blueskyAccount);

        Post post = createPost(PostStatus.PUBLISHING);
        createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);
        createTarget(post.getId(), blueskyAccountId, SnsPlatform.BLUESKY);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));
        snsAdapterLookup.registerAdapter(SnsPlatform.BLUESKY, new SuccessSnsAdapter(SnsPlatform.BLUESKY));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    }

    @Test
    void processPost_marksPublishedWhenPartialSuccess() {
        UUID blueskyAccountId = UUID.randomUUID();
        Instant now = Instant.now();
        SnsAccount blueskyAccount = new SnsAccount(
                blueskyAccountId, userId, SnsPlatform.BLUESKY, "@testuser", "Test User",
                "access_token_bs", "refresh_token_bs", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(blueskyAccount);

        Post post = createPost(PostStatus.PUBLISHING);
        createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);
        createTarget(post.getId(), blueskyAccountId, SnsPlatform.BLUESKY);

        snsAdapterLookup.registerAdapter(SnsPlatform.TWITTER, new SuccessSnsAdapter(SnsPlatform.TWITTER));
        snsAdapterLookup.registerAdapter(SnsPlatform.BLUESKY, new FailingSnsAdapter(SnsPlatform.BLUESKY));

        postPublishUseCase.processPost(post.getId());

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    }

    private Post createPost(PostStatus status) {
        Instant now = Instant.now();
        Post post = new Post(UUID.randomUUID(), userId, "Test content", List.of(),
                status, null, now, now);
        return postRepository.save(post);
    }

    private PostTarget createTarget(UUID postId, UUID snsAccountId, SnsPlatform platform) {
        Instant now = Instant.now();
        PostTarget target = new PostTarget(UUID.randomUUID(), postId, snsAccountId,
                platform, null, PostStatus.PUBLISHING, null, null, now);
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
                    .filter(post -> post.getUserId().equals(userId))
                    .skip(offset).limit(limit).toList();
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

        private boolean callPermitted = true;

        void setCallPermitted(boolean permitted) {
            this.callPermitted = permitted;
        }

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
            return callPermitted;
        }
    }

    private static class SuccessSnsAdapter implements SnsAdapter {

        private final SnsPlatform snsPlatform;

        SuccessSnsAdapter(SnsPlatform snsPlatform) {
            this.snsPlatform = snsPlatform;
        }

        @Override
        public SnsPlatform platform() {
            return snsPlatform;
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
            return new PostResult("platform-post-id-" + UUID.randomUUID(), "https://example.com/post", Instant.now());
        }

        @Override
        public void deletePost(String accessToken, String postId) {
        }

        @Override
        public int getMaxContentLength() {
            return 280;
        }
    }

    private static class FailingSnsAdapter implements SnsAdapter {

        private final SnsPlatform snsPlatform;

        FailingSnsAdapter(SnsPlatform snsPlatform) {
            this.snsPlatform = snsPlatform;
        }

        @Override
        public SnsPlatform platform() {
            return snsPlatform;
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
            throw new RuntimeException("Simulated publish failure");
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
