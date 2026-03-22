package com.akazukin.application.usecase;

import com.akazukin.application.dto.ComplianceCheckResultDto;
import com.akazukin.application.dto.CrossPostRecommendDto;
import com.akazukin.application.dto.PersonaReplyRequestDto;
import com.akazukin.application.dto.PersonaReplyResponseDto;
import com.akazukin.application.dto.PostEngagementDto;
import com.akazukin.application.dto.PostReplyDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.domain.model.Interaction;
import com.akazukin.domain.model.InteractionType;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.InteractionRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAnalyticsAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostDetailUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private InMemorySnsAccountRepository snsAccountRepository;
    private InMemoryInteractionRepository interactionRepository;
    private InMemoryAiPersonaRepository aiPersonaRepository;
    private StubAiTextGenerator aiTextGenerator;
    private StubAgentOrchestrator agentOrchestrator;
    private StubSnsAnalyticsAdapterLookup analyticsAdapters;
    private PostDetailUseCase postDetailUseCase;

    private UUID userId;
    private UUID twitterAccountId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        snsAccountRepository = new InMemorySnsAccountRepository();
        interactionRepository = new InMemoryInteractionRepository();
        aiPersonaRepository = new InMemoryAiPersonaRepository();
        aiTextGenerator = new StubAiTextGenerator();
        agentOrchestrator = new StubAgentOrchestrator();
        analyticsAdapters = new StubSnsAnalyticsAdapterLookup();

        userId = UUID.randomUUID();
        twitterAccountId = UUID.randomUUID();

        Instant now = Instant.now();
        SnsAccount twitterAccount = new SnsAccount(
                twitterAccountId, userId, SnsPlatform.TWITTER, "@testuser", "Test User",
                "access_token", "refresh_token", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(twitterAccount);

        postDetailUseCase = new PostDetailUseCase(
                postRepository, postTargetRepository, snsAccountRepository,
                interactionRepository, aiPersonaRepository, aiTextGenerator,
                agentOrchestrator, analyticsAdapters);
    }

    @Test
    void getEngagement_returnsAggregatedStatsForPublishedPost() {
        Post post = createPublishedPost();
        PostTarget target = createPublishedTarget(post.getId(), twitterAccountId,
                SnsPlatform.TWITTER, "platform-post-1");

        analyticsAdapters.registerAdapter(new StubSnsAnalyticsAdapter(
                SnsPlatform.TWITTER, new SnsPostStats("platform-post-1", SnsPlatform.TWITTER,
                10, 3, 5, 1000, Instant.now())));

        PostEngagementDto engagement = postDetailUseCase.getEngagement(post.getId(), userId);

        assertEquals(10, engagement.likeCount());
        assertEquals(5, engagement.repostCount());
        assertEquals(3, engagement.replyCount());
        assertEquals(1000, engagement.impressionCount());
    }

    @Test
    void getEngagement_returnsZerosWhenNoTargetsHavePlatformPostId() {
        Post post = createPublishedPost();
        createTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER);

        PostEngagementDto engagement = postDetailUseCase.getEngagement(post.getId(), userId);

        assertEquals(0, engagement.likeCount());
        assertEquals(0, engagement.repostCount());
        assertEquals(0, engagement.replyCount());
        assertEquals(0, engagement.impressionCount());
    }

    @Test
    void getEngagement_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postDetailUseCase.getEngagement(nonExistentId, userId));
    }

    @Test
    void getEngagement_returnsZerosWhenNoTargets() {
        Post post = createPublishedPost();

        PostEngagementDto engagement = postDetailUseCase.getEngagement(post.getId(), userId);

        assertEquals(0, engagement.likeCount());
        assertEquals(0, engagement.repostCount());
        assertEquals(0, engagement.replyCount());
        assertEquals(0, engagement.impressionCount());
    }

    @Test
    void getEngagement_skipsTargetsWithMissingAccount() {
        Post post = createPublishedPost();
        UUID missingAccountId = UUID.randomUUID();
        createPublishedTarget(post.getId(), missingAccountId, SnsPlatform.TWITTER, "platform-post-1");

        PostEngagementDto engagement = postDetailUseCase.getEngagement(post.getId(), userId);

        assertEquals(0, engagement.likeCount());
    }

    @Test
    void getCrossPostRecommendations_returnsRecommendationsForUnpostedPlatforms() {
        UUID blueskyAccountId = UUID.randomUUID();
        Instant now = Instant.now();
        SnsAccount blueskyAccount = new SnsAccount(
                blueskyAccountId, userId, SnsPlatform.BLUESKY, "@testuser", "Test User",
                "access_token_bs", "refresh_token_bs", now.plusSeconds(3600), now, now
        );
        snsAccountRepository.save(blueskyAccount);

        Post post = createPublishedPost();
        createPublishedTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER, "platform-post-1");

        agentOrchestrator.setOutput("BLUESKY:score:80:BLUESKY:reason:Short content fits well");

        List<CrossPostRecommendDto> recommendations =
                postDetailUseCase.getCrossPostRecommendations(post.getId(), userId);

        assertNotNull(recommendations);
        assertTrue(recommendations.stream().anyMatch(r -> "BLUESKY".equals(r.platform())));
        assertTrue(recommendations.stream().noneMatch(r -> "TWITTER".equals(r.platform())));
    }

    @Test
    void getCrossPostRecommendations_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postDetailUseCase.getCrossPostRecommendations(nonExistentId, userId));
    }

    @Test
    void getCrossPostRecommendations_returnsEmptyWhenAllPlatformsPosted() {
        Post post = createPublishedPost();
        createPublishedTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER, "platform-post-1");

        agentOrchestrator.setOutput("");

        List<CrossPostRecommendDto> recommendations =
                postDetailUseCase.getCrossPostRecommendations(post.getId(), userId);

        assertTrue(recommendations.stream().noneMatch(r -> "TWITTER".equals(r.platform())));
    }

    @Test
    void getComplianceCheck_returnsDefaultItemsWhenAgentOutputIsEmpty() {
        Post post = createPublishedPost();
        agentOrchestrator.setOutput("");

        ComplianceCheckResultDto result = postDetailUseCase.getComplianceCheck(post.getId(), userId);

        assertNotNull(result);
        assertEquals(6, result.totalCount());
        assertEquals(6, result.passedCount());
        assertTrue(result.allPassed());
    }

    @Test
    void getComplianceCheck_parsesAgentOutput() {
        Post post = createPublishedPost();
        agentOrchestrator.setOutput("景品表示法|優良誤認|PASS|問題なし\n薬機法|効果効能|FAIL|誇大表現あり");

        ComplianceCheckResultDto result = postDetailUseCase.getComplianceCheck(post.getId(), userId);

        assertNotNull(result);
        assertEquals(2, result.totalCount());
        assertEquals(1, result.passedCount());
    }

    @Test
    void getComplianceCheck_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postDetailUseCase.getComplianceCheck(nonExistentId, userId));
    }

    @Test
    void getReplies_returnsRepliesForPost() {
        Post post = createPublishedPost();
        createPublishedTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER, "platform-post-1");

        Instant now = Instant.now();
        Interaction reply = new Interaction(UUID.randomUUID(), userId, twitterAccountId,
                SnsPlatform.TWITTER, InteractionType.REPLY, "platform-post-1",
                "replyuser", "Nice post!", now);
        interactionRepository.save(reply);

        List<PostReplyDto> replies = postDetailUseCase.getReplies(post.getId(), userId, 0, 10);

        assertEquals(1, replies.size());
        assertEquals("Nice post!", replies.get(0).content());
    }

    @Test
    void getReplies_excludesNonReplyInteractions() {
        Post post = createPublishedPost();
        createPublishedTarget(post.getId(), twitterAccountId, SnsPlatform.TWITTER, "platform-post-1");

        Instant now = Instant.now();
        Interaction like = new Interaction(UUID.randomUUID(), userId, twitterAccountId,
                SnsPlatform.TWITTER, InteractionType.LIKE, "platform-post-1",
                "likeuser", null, now);
        interactionRepository.save(like);

        List<PostReplyDto> replies = postDetailUseCase.getReplies(post.getId(), userId, 0, 10);

        assertTrue(replies.isEmpty());
    }

    @Test
    void getReplies_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> postDetailUseCase.getReplies(nonExistentId, userId, 0, 10));
    }

    @Test
    void getReplies_returnsEmptyWhenNoTargets() {
        Post post = createPublishedPost();

        List<PostReplyDto> replies = postDetailUseCase.getReplies(post.getId(), userId, 0, 10);

        assertTrue(replies.isEmpty());
    }

    @Test
    void suggestReply_returnsGeneratedReplyWithPersona() {
        Post post = createPublishedPost();

        UUID personaId = UUID.randomUUID();
        Instant now = Instant.now();
        AiPersona persona = new AiPersona(personaId, userId, "Friendly Bot",
                "You are a friendly assistant", ContentTone.FRIENDLY, "ja",
                null, true, now, now);
        aiPersonaRepository.save(persona);

        aiTextGenerator.setResponse(new AiResponse("Thanks for your comment!", 50, 100, "test-model"));

        PersonaReplyRequestDto request = new PersonaReplyRequestDto(
                UUID.randomUUID(), personaId, "Great post!");

        PersonaReplyResponseDto response = postDetailUseCase.suggestReply(post.getId(), userId, request);

        assertEquals("Friendly Bot", response.personaName());
        assertEquals("Thanks for your comment!", response.generatedReply());
    }

    @Test
    void suggestReply_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentId = UUID.randomUUID();
        UUID personaId = UUID.randomUUID();
        PersonaReplyRequestDto request = new PersonaReplyRequestDto(
                UUID.randomUUID(), personaId, "content");

        assertThrows(PostNotFoundException.class,
                () -> postDetailUseCase.suggestReply(nonExistentId, userId, request));
    }

    @Test
    void suggestReply_throwsPersonaNotFoundForNonExistentPersona() {
        Post post = createPublishedPost();
        UUID nonExistentPersonaId = UUID.randomUUID();
        PersonaReplyRequestDto request = new PersonaReplyRequestDto(
                UUID.randomUUID(), nonExistentPersonaId, "content");

        DomainException exception = assertThrows(DomainException.class,
                () -> postDetailUseCase.suggestReply(post.getId(), userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    private Post createPublishedPost() {
        Instant now = Instant.now();
        Post post = new Post(UUID.randomUUID(), userId, "Test content", List.of(),
                PostStatus.PUBLISHED, null, now, now);
        return postRepository.save(post);
    }

    private PostTarget createTarget(UUID postId, UUID snsAccountId, SnsPlatform platform) {
        Instant now = Instant.now();
        PostTarget target = new PostTarget(UUID.randomUUID(), postId, snsAccountId,
                platform, null, PostStatus.PUBLISHED, null, null, now);
        return postTargetRepository.save(target);
    }

    private PostTarget createPublishedTarget(UUID postId, UUID snsAccountId,
                                             SnsPlatform platform, String platformPostId) {
        Instant now = Instant.now();
        PostTarget target = new PostTarget(UUID.randomUUID(), postId, snsAccountId,
                platform, platformPostId, PostStatus.PUBLISHED, null, now, now);
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
            return List.of();
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

    private static class InMemoryInteractionRepository implements InteractionRepository {

        private final Map<UUID, Interaction> store = new HashMap<>();

        @Override
        public List<Interaction> findByUserId(UUID userId, int offset, int limit) {
            return store.values().stream()
                    .filter(i -> i.getUserId().equals(userId))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public List<Interaction> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
            return store.values().stream()
                    .filter(i -> i.getSnsAccountId().equals(snsAccountId))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public List<Interaction> findByUserIdAndTargetUserId(UUID userId, String targetUserId, int offset, int limit) {
            return store.values().stream()
                    .filter(i -> i.getUserId().equals(userId) && targetUserId.equals(i.getTargetUserId()))
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public Interaction save(Interaction interaction) {
            store.put(interaction.getId(), interaction);
            return interaction;
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.values().stream().filter(i -> i.getUserId().equals(userId)).count();
        }

        @Override
        public long countByUserIdAndTargetUserId(UUID userId, String targetUserId) {
            return store.values().stream()
                    .filter(i -> i.getUserId().equals(userId) && targetUserId.equals(i.getTargetUserId()))
                    .count();
        }
    }

    private static class InMemoryAiPersonaRepository implements AiPersonaRepository {

        private final Map<UUID, AiPersona> store = new HashMap<>();

        @Override
        public Optional<AiPersona> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<AiPersona> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId)).toList();
        }

        @Override
        public Optional<AiPersona> findDefaultByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId) && p.isDefault())
                    .findFirst();
        }

        @Override
        public AiPersona save(AiPersona aiPersona) {
            store.put(aiPersona.getId(), aiPersona);
            return aiPersona;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }

    private static class StubAiTextGenerator implements AiTextGenerator {

        private AiResponse response = new AiResponse("default reply", 10, 50, "test-model");

        void setResponse(AiResponse response) {
            this.response = response;
        }

        @Override
        public AiResponse generate(AiPrompt prompt) {
            return response;
        }

        @Override
        public AiResponse generateWithPersona(AiPersona persona, String userInput) {
            return response;
        }
    }

    private static class StubAgentOrchestrator implements AgentOrchestrator {

        private String output = "";

        void setOutput(String output) {
            this.output = output;
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input) {
            return new AgentTask(UUID.randomUUID(), userId, agentType, input, output,
                    "COMPLETED", null, Instant.now(), Instant.now());
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId) {
            return new AgentTask(UUID.randomUUID(), userId, agentType, input, output,
                    "COMPLETED", parentTaskId, Instant.now(), Instant.now());
        }

        @Override
        public AgentTask getTaskResult(UUID taskId) {
            return new AgentTask(taskId, UUID.randomUUID(), AgentType.ANALYST, "", output,
                    "COMPLETED", null, Instant.now(), Instant.now());
        }
    }

    private static class StubSnsAnalyticsAdapter implements SnsAnalyticsAdapter {

        private final SnsPlatform snsPlatform;
        private final SnsPostStats stats;

        StubSnsAnalyticsAdapter(SnsPlatform snsPlatform, SnsPostStats stats) {
            this.snsPlatform = snsPlatform;
            this.stats = stats;
        }

        @Override
        public SnsPlatform platform() {
            return snsPlatform;
        }

        @Override
        public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
            if (stats != null && stats.platformPostId().equals(platformPostId)) {
                return Optional.of(stats);
            }
            return Optional.empty();
        }

        @Override
        public Optional<AccountStats> getAccountStats(String accessToken) {
            return Optional.empty();
        }
    }

    private static class StubSnsAnalyticsAdapterLookup implements SnsAnalyticsAdapterLookup {

        private final Map<SnsPlatform, SnsAnalyticsAdapter> adapterMap = new EnumMap<>(SnsPlatform.class);

        void registerAdapter(SnsAnalyticsAdapter adapter) {
            adapterMap.put(adapter.platform(), adapter);
        }

        @Override
        public SnsAnalyticsAdapter getAdapter(SnsPlatform platform) {
            SnsAnalyticsAdapter adapter = adapterMap.get(platform);
            if (adapter == null) {
                throw new IllegalArgumentException("No analytics adapter for platform: " + platform);
            }
            return adapter;
        }

        @Override
        public Collection<SnsAnalyticsAdapter> getAllAdapters() {
            return adapterMap.values();
        }

        @Override
        public boolean supports(SnsPlatform platform) {
            return adapterMap.containsKey(platform);
        }
    }
}
