package com.akazukin.application.usecase;

import com.akazukin.application.dto.AgentPerformanceDto;
import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.application.dto.PersonaPerformanceDto;
import com.akazukin.application.dto.PlatformSuccessRateDto;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.domain.model.DashboardSummary;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.port.AgentTaskRepository;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.ImpressionSnapshotRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAnalyticsAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private InMemorySnsAccountRepository snsAccountRepository;
    private InMemoryAgentTaskRepository agentTaskRepository;
    private InMemoryAiPersonaRepository aiPersonaRepository;
    private InMemoryImpressionSnapshotRepository impressionSnapshotRepository;
    private StubSnsAnalyticsAdapter twitterAnalyticsAdapter;
    private AnalyticsUseCase analyticsUseCase;

    private UUID userId;
    private UUID twitterAccountId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        snsAccountRepository = new InMemorySnsAccountRepository();
        agentTaskRepository = new InMemoryAgentTaskRepository();
        aiPersonaRepository = new InMemoryAiPersonaRepository();
        impressionSnapshotRepository = new InMemoryImpressionSnapshotRepository();

        twitterAnalyticsAdapter = new StubSnsAnalyticsAdapter(SnsPlatform.TWITTER);

        SnsAnalyticsAdapterLookup adapterLookup = new StubSnsAnalyticsAdapterLookup(
                List.of(twitterAnalyticsAdapter));

        analyticsUseCase = new AnalyticsUseCase(
                postRepository, postTargetRepository, snsAccountRepository,
                agentTaskRepository, aiPersonaRepository, impressionSnapshotRepository,
                adapterLookup);

        userId = UUID.randomUUID();
        twitterAccountId = UUID.randomUUID();

        Instant now = Instant.now();
        SnsAccount twitterAccount = new SnsAccount(
                twitterAccountId, userId, SnsPlatform.TWITTER, "@testuser", "Test User",
                "access_token", "refresh_token", now.plusSeconds(3600), now, now);
        snsAccountRepository.save(twitterAccount);

        twitterAnalyticsAdapter.setAccountStats(new AccountStats(
                SnsPlatform.TWITTER, "@testuser", 1000, 500, 200, now));
    }

    @Test
    void getDashboardSummary_returnsCorrectCounts() {
        Instant now = Instant.now();
        postRepository.save(new Post(UUID.randomUUID(), userId, "Post 1", List.of(),
                PostStatus.PUBLISHED, null, now, now));
        postRepository.save(new Post(UUID.randomUUID(), userId, "Post 2", List.of(),
                PostStatus.PUBLISHED, null, now, now));
        postRepository.save(new Post(UUID.randomUUID(), userId, "Post 3", List.of(),
                PostStatus.FAILED, null, now, now));
        postRepository.save(new Post(UUID.randomUUID(), userId, "Post 4", List.of(),
                PostStatus.SCHEDULED, now.plusSeconds(3600), now, now));

        DashboardSummary summary = analyticsUseCase.getDashboardSummary(userId);

        assertEquals(4, summary.totalPosts());
        assertEquals(2, summary.publishedPosts());
        assertEquals(1, summary.failedPosts());
        assertEquals(1, summary.scheduledPosts());
        assertEquals(1, summary.connectedAccounts());
    }

    @Test
    void getDashboardSummary_returnsZeroCountsForNewUser() {
        UUID newUserId = UUID.randomUUID();

        DashboardSummary summary = analyticsUseCase.getDashboardSummary(newUserId);

        assertEquals(0, summary.totalPosts());
        assertEquals(0, summary.publishedPosts());
        assertEquals(0, summary.failedPosts());
        assertEquals(0, summary.scheduledPosts());
        assertEquals(0, summary.connectedAccounts());
    }

    @Test
    void getAnalytics_returnsResponseWithPlatformCounts() {
        Instant now = Instant.now();
        postRepository.save(new Post(UUID.randomUUID(), userId, "Post 1", List.of(),
                PostStatus.PUBLISHED, null, now, now));

        AnalyticsResponseDto analytics = analyticsUseCase.getAnalytics(userId);

        assertNotNull(analytics);
        assertEquals(1, analytics.totalPosts());
        assertEquals(1, analytics.publishedPosts());
        assertEquals(1, analytics.connectedAccounts());
    }

    @Test
    void getAnalytics_returnsEmptyForNewUser() {
        UUID newUserId = UUID.randomUUID();

        AnalyticsResponseDto analytics = analyticsUseCase.getAnalytics(newUserId);

        assertEquals(0, analytics.totalPosts());
        assertEquals(0, analytics.connectedAccounts());
    }

    @Test
    void getPostAnalytics_throwsPostNotFoundForNonExistentPost() {
        UUID nonExistentPostId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> analyticsUseCase.getPostAnalytics(nonExistentPostId));
    }

    @Test
    void getPostAnalytics_returnsEmptyWhenNoTargetsHavePlatformPostId() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        postRepository.save(new Post(postId, userId, "Hello", List.of(),
                PostStatus.PUBLISHED, null, now, now));
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, null, PostStatus.PUBLISHING, null, null, now));

        List<SnsPostStats> stats = analyticsUseCase.getPostAnalytics(postId);

        assertTrue(stats.isEmpty());
    }

    @Test
    void getPostAnalytics_returnsStatsForPublishedTarget() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        postRepository.save(new Post(postId, userId, "Hello", List.of(),
                PostStatus.PUBLISHED, null, now, now));

        String platformPostId = "tweet_12345";
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, platformPostId, PostStatus.PUBLISHED, null, now, now));

        twitterAnalyticsAdapter.setPostStats(platformPostId,
                new SnsPostStats(platformPostId, SnsPlatform.TWITTER, 10, 3, 5, 1000, now));

        List<SnsPostStats> stats = analyticsUseCase.getPostAnalytics(postId);

        assertEquals(1, stats.size());
        assertEquals(10, stats.get(0).likeCount());
    }

    @Test
    void getAgentPerformance_returnsPerformanceForAllAgentTypes() {
        Instant now = Instant.now();
        agentTaskRepository.save(new AgentTask(UUID.randomUUID(), userId, AgentType.DIRECTOR,
                "input", "output", "COMPLETED", null, now.minus(5, ChronoUnit.DAYS),
                now.minus(5, ChronoUnit.DAYS).plusSeconds(10)));
        agentTaskRepository.save(new AgentTask(UUID.randomUUID(), userId, AgentType.DIRECTOR,
                "input", null, "FAILED", null, now.minus(3, ChronoUnit.DAYS), null));

        List<AgentPerformanceDto> performances = analyticsUseCase.getAgentPerformance(userId);

        assertNotNull(performances);
        assertEquals(AgentType.values().length, performances.size());

        AgentPerformanceDto directorPerf = performances.stream()
                .filter(p -> "DIRECTOR".equals(p.agentType()))
                .findFirst().orElseThrow();
        assertEquals(2, directorPerf.totalTasks());
        assertEquals(1, directorPerf.completedTasks());
        assertEquals(1, directorPerf.failedTasks());
        assertEquals(50.0, directorPerf.successRate());
    }

    @Test
    void getAgentPerformance_returnsZeroRatesForNewUser() {
        UUID newUserId = UUID.randomUUID();

        List<AgentPerformanceDto> performances = analyticsUseCase.getAgentPerformance(newUserId);

        assertEquals(AgentType.values().length, performances.size());
        for (AgentPerformanceDto perf : performances) {
            assertEquals(0, perf.totalTasks());
            assertEquals(0.0, perf.successRate());
        }
    }

    @Test
    void getPersonaPerformance_returnsPerformancePerPersona() {
        Instant now = Instant.now();
        AiPersona persona = new AiPersona(UUID.randomUUID(), userId, "Business",
                "formal system prompt", ContentTone.FORMAL, "ja", null, true, now, now);
        aiPersonaRepository.save(persona);

        impressionSnapshotRepository.save(new ImpressionSnapshot(UUID.randomUUID(),
                twitterAccountId, SnsPlatform.TWITTER, 1000, 500, 200, 50000, 3.5, now));

        List<PersonaPerformanceDto> performances = analyticsUseCase.getPersonaPerformance(userId);

        assertEquals(1, performances.size());
        assertEquals("Business", performances.get(0).personaName());
    }

    @Test
    void getPersonaPerformance_returnsEmptyWhenNoPersonas() {
        List<PersonaPerformanceDto> performances = analyticsUseCase.getPersonaPerformance(userId);

        assertTrue(performances.isEmpty());
    }

    @Test
    void getCrossPlatformHeatmap_returnsHeatmapWithThemesAndPlatforms() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        postRepository.save(new Post(postId, userId, "#tech New technology",
                List.of(), PostStatus.PUBLISHED, null, now, now));
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, "tweet_1", PostStatus.PUBLISHED, null, now, now));

        var heatmap = analyticsUseCase.getCrossPlatformHeatmap(userId);

        assertNotNull(heatmap);
        assertTrue(heatmap.themes().contains("TECH"));
        assertTrue(heatmap.platforms().contains("TWITTER"));
    }

    @Test
    void getCrossPlatformHeatmap_returnsEmptyWhenNoPosts() {
        UUID newUserId = UUID.randomUUID();

        var heatmap = analyticsUseCase.getCrossPlatformHeatmap(newUserId);

        assertNotNull(heatmap);
        assertTrue(heatmap.themes().isEmpty());
        assertTrue(heatmap.platforms().isEmpty());
        assertTrue(heatmap.cells().isEmpty());
    }

    @Test
    void getTrendTimeline_returnsSuccessRatesPerPlatform() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, "tweet_1", PostStatus.PUBLISHED, null, now, now));
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, null, PostStatus.FAILED, "error", null, now));

        List<PlatformSuccessRateDto> timeline = analyticsUseCase.getTrendTimeline(userId, 7);

        assertNotNull(timeline);
        assertEquals(1, timeline.size());
        assertEquals("TWITTER", timeline.get(0).platform());
        assertEquals(50.0, timeline.get(0).overallRate());
    }

    @Test
    void getTrendTimeline_returnsEmptyWhenNoTargets() {
        List<PlatformSuccessRateDto> timeline = analyticsUseCase.getTrendTimeline(userId, 7);

        assertTrue(timeline.isEmpty());
    }

    @Test
    void getPlatformCorrelation_returnsSelfCorrelation() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        postTargetRepository.save(new PostTarget(UUID.randomUUID(), postId, twitterAccountId,
                SnsPlatform.TWITTER, "tweet_1", PostStatus.PUBLISHED, null, now, now));

        var correlation = analyticsUseCase.getPlatformCorrelation(userId);

        assertNotNull(correlation);
        assertTrue(correlation.platforms().contains("TWITTER"));
        assertEquals(1, correlation.cells().stream()
                .filter(c -> c.platformA().equals("TWITTER") && c.platformB().equals("TWITTER"))
                .count());
    }

    @Test
    void getRiskAnalysis_returnsEmptyTrendsForNewUser() {
        UUID newUserId = UUID.randomUUID();

        var riskAnalysis = analyticsUseCase.getRiskAnalysis(newUserId);

        assertNotNull(riskAnalysis);
        assertTrue(riskAnalysis.trendPoints().isEmpty());
        assertTrue(riskAnalysis.categoryRanking().isEmpty());
    }

    @Test
    void getRiskAnalysis_classifiesRiskCategories() {
        Instant now = Instant.now();
        agentTaskRepository.save(new AgentTask(UUID.randomUUID(), userId, AgentType.SENTINEL,
                "check post", "copyright violation detected", "COMPLETED", null,
                now.minus(10, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS).plusSeconds(5)));
        agentTaskRepository.save(new AgentTask(UUID.randomUUID(), userId, AgentType.COMPLIANCE,
                "check post", "privacy concern found", "COMPLETED", null,
                now.minus(10, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS).plusSeconds(5)));

        var riskAnalysis = analyticsUseCase.getRiskAnalysis(userId);

        assertNotNull(riskAnalysis);
        assertTrue(riskAnalysis.categoryRanking().stream()
                .anyMatch(r -> "COPYRIGHT".equals(r.category())));
        assertTrue(riskAnalysis.categoryRanking().stream()
                .anyMatch(r -> "PRIVACY".equals(r.category())));
    }

    @Test
    void getSeasonalAnalysis_returnsDataPointsByPlatform() {
        Instant now = Instant.now();
        impressionSnapshotRepository.save(new ImpressionSnapshot(UUID.randomUUID(),
                twitterAccountId, SnsPlatform.TWITTER, 1000, 500, 200, 50000, 3.5, now));

        var seasonal = analyticsUseCase.getSeasonalAnalysis(userId);

        assertNotNull(seasonal);
        assertTrue(seasonal.platforms().contains("TWITTER"));
    }

    @Test
    void getSeasonalAnalysis_returnsEmptyForNewUser() {
        UUID newUserId = UUID.randomUUID();

        var seasonal = analyticsUseCase.getSeasonalAnalysis(newUserId);

        assertNotNull(seasonal);
        assertTrue(seasonal.platforms().isEmpty());
        assertTrue(seasonal.dataPoints().isEmpty());
    }

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
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .count();
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, PostStatus status) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .filter(p -> p.getStatus() == status)
                    .count();
        }

        @Override
        public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
            return new EnumMap<>(SnsPlatform.class);
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
                    .filter(t -> t.getPostId().equals(postId))
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
                    .filter(t -> t.getPostId().equals(postId))
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
                    .filter(t -> postIds.contains(t.getPostId()))
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

        @Override
        public Map<String, Long> countByStatusForUser(UUID userId) {
            return Map.of();
        }

        @Override
        public List<PostTarget> findByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to) {
            return store.values().stream()
                    .filter(t -> {
                        Instant createdAt = t.getCreatedAt();
                        return createdAt != null && !createdAt.isBefore(from) && !createdAt.isAfter(to);
                    })
                    .toList();
        }

        @Override
        public List<PostTarget> findByUserIdAndPlatformAndCreatedAtBetween(UUID userId, SnsPlatform platform, Instant from, Instant to) {
            return store.values().stream()
                    .filter(t -> t.getPlatform() == platform)
                    .filter(t -> {
                        Instant createdAt = t.getCreatedAt();
                        return createdAt != null && !createdAt.isBefore(from) && !createdAt.isAfter(to);
                    })
                    .toList();
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
                    .filter(a -> a.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
            return store.values().stream()
                    .filter(a -> a.getUserId().equals(userId))
                    .filter(a -> a.getPlatform() == platform)
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
                    .filter(a -> ids.contains(a.getId()))
                    .toList();
        }

        @Override
        public long countByPlatform(SnsPlatform platform) {
            return store.values().stream()
                    .filter(a -> a.getPlatform() == platform)
                    .count();
        }

        @Override
        public long countAll() {
            return store.size();
        }
    }

    private static class InMemoryAgentTaskRepository implements AgentTaskRepository {

        private final Map<UUID, AgentTask> store = new HashMap<>();

        @Override
        public Optional<AgentTask> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<AgentTask> findByUserId(UUID userId, int offset, int limit) {
            List<AgentTask> tasks = store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .toList();
            int end = Math.min(offset + limit, tasks.size());
            if (offset >= tasks.size()) return List.of();
            return new ArrayList<>(tasks.subList(offset, end));
        }

        @Override
        public List<AgentTask> findByParentTaskId(UUID parentTaskId) {
            return store.values().stream()
                    .filter(t -> parentTaskId.equals(t.getParentTaskId()))
                    .toList();
        }

        @Override
        public AgentTask save(AgentTask task) {
            store.put(task.getId(), task);
            return task;
        }

        @Override
        public void updateStatus(UUID id, String status, String output) {
            AgentTask task = store.get(id);
            if (task != null) {
                task.setStatus(status);
                task.setOutput(output);
            }
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .count();
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, String status) {
            return store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .filter(t -> status.equals(t.getStatus()))
                    .count();
        }

        @Override
        public List<AgentTask> findByUserIdOrderByCreatedAt(UUID userId, int offset, int limit) {
            return findByUserId(userId, offset, limit);
        }

        @Override
        public Map<AgentType, Long> countByUserIdGroupByAgentType(UUID userId) {
            return store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .collect(Collectors.groupingBy(AgentTask::getAgentType, Collectors.counting()));
        }

        @Override
        public Map<AgentType, Long> countByUserIdAndStatusGroupByAgentType(UUID userId, String status) {
            return store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .filter(t -> status.equals(t.getStatus()))
                    .collect(Collectors.groupingBy(AgentTask::getAgentType, Collectors.counting()));
        }

        @Override
        public List<AgentTask> findByUserIdAndCreatedAtAfter(UUID userId, Instant after) {
            return store.values().stream()
                    .filter(t -> t.getUserId().equals(userId))
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(after))
                    .toList();
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
                    .filter(p -> p.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<AiPersona> findDefaultByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .filter(AiPersona::isDefault)
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

    private static class InMemoryImpressionSnapshotRepository implements ImpressionSnapshotRepository {

        private final List<ImpressionSnapshot> store = new ArrayList<>();

        @Override
        public List<ImpressionSnapshot> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
            List<ImpressionSnapshot> filtered = store.stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .toList();
            int end = Math.min(offset + limit, filtered.size());
            if (offset >= filtered.size()) return List.of();
            return new ArrayList<>(filtered.subList(offset, end));
        }

        @Override
        public List<ImpressionSnapshot> findBySnsAccountIdAndDateRange(UUID snsAccountId, Instant from, Instant to) {
            return store.stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .filter(s -> !s.getSnapshotAt().isBefore(from) && !s.getSnapshotAt().isAfter(to))
                    .toList();
        }

        @Override
        public ImpressionSnapshot save(ImpressionSnapshot snapshot) {
            store.add(snapshot);
            return snapshot;
        }

        @Override
        public Optional<ImpressionSnapshot> getLatestBySnsAccountId(UUID snsAccountId) {
            return store.stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .max((a, b) -> a.getSnapshotAt().compareTo(b.getSnapshotAt()));
        }

        @Override
        public List<ImpressionSnapshot> findRecentByAccountIds(List<UUID> snsAccountIds, int limit) {
            return store.stream()
                    .filter(s -> snsAccountIds.contains(s.getSnsAccountId()))
                    .sorted((a, b) -> b.getSnapshotAt().compareTo(a.getSnapshotAt()))
                    .limit(limit)
                    .toList();
        }
    }

    private static class StubSnsAnalyticsAdapter implements SnsAnalyticsAdapter {

        private final SnsPlatform targetPlatform;
        private AccountStats accountStats;
        private final Map<String, SnsPostStats> postStatsMap = new HashMap<>();

        StubSnsAnalyticsAdapter(SnsPlatform platform) {
            this.targetPlatform = platform;
        }

        void setAccountStats(AccountStats stats) {
            this.accountStats = stats;
        }

        void setPostStats(String platformPostId, SnsPostStats stats) {
            postStatsMap.put(platformPostId, stats);
        }

        @Override
        public SnsPlatform platform() {
            return targetPlatform;
        }

        @Override
        public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
            return Optional.ofNullable(postStatsMap.get(platformPostId));
        }

        @Override
        public Optional<AccountStats> getAccountStats(String accessToken) {
            return Optional.ofNullable(accountStats);
        }
    }

    private static class StubSnsAnalyticsAdapterLookup implements SnsAnalyticsAdapterLookup {

        private final Map<SnsPlatform, SnsAnalyticsAdapter> adapterMap;

        StubSnsAnalyticsAdapterLookup(List<SnsAnalyticsAdapter> adapters) {
            this.adapterMap = new EnumMap<>(SnsPlatform.class);
            for (SnsAnalyticsAdapter adapter : adapters) {
                this.adapterMap.put(adapter.platform(), adapter);
            }
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
