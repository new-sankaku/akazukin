package com.akazukin.application.usecase;

import com.akazukin.application.dto.FireWatchSummaryDto;
import com.akazukin.application.dto.PlatformSuccessRateDto;
import com.akazukin.application.dto.PodMonitorDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.QueueStatusDto;
import com.akazukin.application.dto.TrendWordDto;
import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.model.DashboardSummary;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AuditLogRepository;
import com.akazukin.domain.port.ImpressionSnapshotRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardUseCaseTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostTargetRepository postTargetRepository;
    private InMemorySnsAccountRepository snsAccountRepository;
    private InMemoryAuditLogRepository auditLogRepository;
    private InMemoryImpressionSnapshotRepository impressionSnapshotRepository;
    private DashboardUseCase dashboardUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        postTargetRepository = new InMemoryPostTargetRepository();
        snsAccountRepository = new InMemorySnsAccountRepository();
        auditLogRepository = new InMemoryAuditLogRepository();
        impressionSnapshotRepository = new InMemoryImpressionSnapshotRepository();
        dashboardUseCase = new DashboardUseCase(
                postRepository, postTargetRepository, snsAccountRepository,
                auditLogRepository, impressionSnapshotRepository
        );
        userId = UUID.randomUUID();
    }

    @Test
    void getSummary_returnsCorrectCounts() {
        Instant now = Instant.now();
        postRepository.save(createPost(userId, "Published", PostStatus.PUBLISHED, now));
        postRepository.save(createPost(userId, "Published2", PostStatus.PUBLISHED, now));
        postRepository.save(createPost(userId, "Failed", PostStatus.FAILED, now));
        postRepository.save(createPost(userId, "Scheduled", PostStatus.SCHEDULED, now));

        DashboardSummary result = dashboardUseCase.getSummary(userId);

        assertEquals(4, result.totalPosts());
        assertEquals(2, result.publishedPosts());
        assertEquals(1, result.failedPosts());
        assertEquals(1, result.scheduledPosts());
    }

    @Test
    void getSummary_returnsZerosForUserWithNoPosts() {
        DashboardSummary result = dashboardUseCase.getSummary(userId);

        assertEquals(0, result.totalPosts());
        assertEquals(0, result.publishedPosts());
        assertEquals(0, result.failedPosts());
        assertEquals(0, result.scheduledPosts());
    }

    @Test
    void getSummary_countsConnectedAccounts() {
        Instant now = Instant.now();
        snsAccountRepository.save(new SnsAccount(
                UUID.randomUUID(), userId, SnsPlatform.TWITTER, "@user", "User",
                "token", "refresh", now.plusSeconds(3600), now, now
        ));
        snsAccountRepository.save(new SnsAccount(
                UUID.randomUUID(), userId, SnsPlatform.BLUESKY, "@user.bsky", "User",
                "token", "refresh", now.plusSeconds(3600), now, now
        ));

        DashboardSummary result = dashboardUseCase.getSummary(userId);

        assertEquals(2, result.connectedAccounts());
    }

    @Test
    void getRecentPosts_returnsPostsWithTargets() {
        Instant now = Instant.now();
        Post post = createPost(userId, "Hello #test", PostStatus.PUBLISHED, now);
        postRepository.save(post);

        PostTarget target = new PostTarget(
                UUID.randomUUID(), post.getId(), UUID.randomUUID(), SnsPlatform.TWITTER,
                "tw-123", PostStatus.PUBLISHED, null, now, now
        );
        postTargetRepository.save(target);

        List<PostResponseDto> result = dashboardUseCase.getRecentPosts(userId, 10);

        assertEquals(1, result.size());
        assertEquals("Hello #test", result.get(0).content());
        assertEquals(1, result.get(0).targets().size());
        assertEquals("TWITTER", result.get(0).targets().get(0).platform());
    }

    @Test
    void getRecentPosts_respectsLimit() {
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            postRepository.save(createPost(userId, "Post " + i, PostStatus.PUBLISHED, now));
        }

        List<PostResponseDto> result = dashboardUseCase.getRecentPosts(userId, 3);

        assertEquals(3, result.size());
    }

    @Test
    void getRecentPosts_returnsEmptyForUserWithNoPosts() {
        List<PostResponseDto> result = dashboardUseCase.getRecentPosts(userId, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimelineSummary_returnsSummaryAndRecentPosts() {
        Instant now = Instant.now();
        postRepository.save(createPost(userId, "Post 1", PostStatus.PUBLISHED, now));

        Map<String, Object> result = dashboardUseCase.getTimelineSummary(userId);

        assertNotNull(result.get("summary"));
        assertNotNull(result.get("recentPosts"));
        assertTrue(result.get("summary") instanceof DashboardSummary);
    }

    @Test
    void getPodMonitorLog_returnsLogsAndStatusCounts() {
        Instant now = Instant.now();
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "GET", "/api/twitter/posts",
                null, null, 200, 50, "127.0.0.1", "agent", now, "post"
        ));
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "POST", "/api/bluesky/posts",
                null, null, 500, 120, "127.0.0.1", "agent", now, "post"
        ));

        PodMonitorDto result = dashboardUseCase.getPodMonitorLog(10);

        assertEquals(2, result.entries().size());
    }

    @Test
    void getPodMonitorLog_extractsPlatformFromPath() {
        Instant now = Instant.now();
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "GET", "/api/twitter/posts",
                null, null, 200, 50, "127.0.0.1", "agent", now, "post"
        ));

        PodMonitorDto result = dashboardUseCase.getPodMonitorLog(10);

        assertEquals("twitter", result.entries().get(0).platform());
    }

    @Test
    void getPodMonitorLog_resolvesLogLevelFromStatusCode() {
        Instant now = Instant.now();
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "GET", "/api/posts",
                null, null, 200, 10, "127.0.0.1", "agent", now, "post"
        ));
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "GET", "/api/posts",
                null, null, 404, 10, "127.0.0.1", "agent", now, "post"
        ));
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(), userId, "user", "GET", "/api/posts",
                null, null, 500, 10, "127.0.0.1", "agent", now, "post"
        ));

        PodMonitorDto result = dashboardUseCase.getPodMonitorLog(10);

        List<String> levels = result.entries().stream()
                .map(e -> e.level())
                .sorted()
                .toList();

        assertTrue(levels.contains("ok"));
        assertTrue(levels.contains("warn"));
        assertTrue(levels.contains("err"));
    }

    @Test
    void getPodMonitorLog_returnsEmptyWhenNoLogs() {
        PodMonitorDto result = dashboardUseCase.getPodMonitorLog(10);

        assertTrue(result.entries().isEmpty());
    }

    @Test
    void getQueueStatus_returnsCorrectStatusCounts() {
        Instant now = Instant.now();
        UUID postId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        postTargetRepository.save(new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.TWITTER,
                null, PostStatus.SCHEDULED, null, null, now
        ));
        postTargetRepository.save(new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.BLUESKY,
                null, PostStatus.PUBLISHING, null, null, now
        ));
        postTargetRepository.save(new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.MASTODON,
                "m-123", PostStatus.PUBLISHED, null, now, now
        ));
        postTargetRepository.save(new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.THREADS,
                null, PostStatus.FAILED, "error", null, now
        ));

        postTargetRepository.setUserIdForAll(userId);

        QueueStatusDto result = dashboardUseCase.getQueueStatus(userId);

        assertEquals(1, result.pending());
        assertEquals(1, result.processing());
        assertEquals(1, result.completed());
        assertEquals(1, result.failed());
    }

    @Test
    void getQueueStatus_returnsZerosForUserWithNoTargets() {
        QueueStatusDto result = dashboardUseCase.getQueueStatus(userId);

        assertEquals(0, result.pending());
        assertEquals(0, result.processing());
        assertEquals(0, result.completed());
        assertEquals(0, result.failed());
    }

    @Test
    void getTrendWords_extractsHashtags() {
        Instant now = Instant.now();
        postRepository.save(createPost(userId, "Hello #spring #spring #spring", PostStatus.PUBLISHED, now));
        postRepository.save(createPost(userId, "World #spring #summer", PostStatus.PUBLISHED, now));

        List<TrendWordDto> result = dashboardUseCase.getTrendWords(userId);

        assertFalse(result.isEmpty());
        assertEquals("#spring", result.get(0).word());
        assertEquals(1, result.get(0).rank());
    }

    @Test
    void getTrendWords_returnsEmptyForUserWithNoPosts() {
        List<TrendWordDto> result = dashboardUseCase.getTrendWords(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTrendWords_limitsToEightWords() {
        Instant now = Instant.now();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            content.append("#tag").append(i).append(" ");
        }
        postRepository.save(createPost(userId, content.toString(), PostStatus.PUBLISHED, now));

        List<TrendWordDto> result = dashboardUseCase.getTrendWords(userId);

        assertTrue(result.size() <= 8);
    }

    @Test
    void getTrendWords_calculatesAffinity() {
        Instant now = Instant.now();
        postRepository.save(createPost(userId, "#top #top #top #mid #mid #low", PostStatus.PUBLISHED, now));

        List<TrendWordDto> result = dashboardUseCase.getTrendWords(userId);

        assertFalse(result.isEmpty());
        assertEquals("high", result.get(0).affinity());
    }

    @Test
    void getPlatformTimeline_returnsRatesGroupedByPlatform() {
        Instant pastTime = Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
        UUID postId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        PostTarget publishedTarget = new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.TWITTER,
                "tw-1", PostStatus.PUBLISHED, null, pastTime, pastTime
        );
        PostTarget failedTarget = new PostTarget(
                UUID.randomUUID(), postId, accountId, SnsPlatform.TWITTER,
                null, PostStatus.FAILED, "error", null, pastTime
        );

        postTargetRepository.save(publishedTarget);
        postTargetRepository.save(failedTarget);

        postTargetRepository.setUserIdForAll(userId);

        List<PlatformSuccessRateDto> result = dashboardUseCase.getPlatformTimeline(userId, 7);

        assertEquals(1, result.size());
        assertEquals("TWITTER", result.get(0).platform());
        assertEquals(50.0, result.get(0).overallRate(), 0.01);
    }

    @Test
    void getPlatformTimeline_returnsEmptyForNoTargets() {
        List<PlatformSuccessRateDto> result = dashboardUseCase.getPlatformTimeline(userId, 7);

        assertTrue(result.isEmpty());
    }

    @Test
    void getFireWatchSummary_classifiesNormalWhenSingleSnapshot() {
        Instant now = Instant.now();
        UUID accountId = UUID.randomUUID();

        snsAccountRepository.save(new SnsAccount(
                accountId, userId, SnsPlatform.TWITTER, "@user", "User",
                "token", "refresh", now.plusSeconds(3600), now, now
        ));

        impressionSnapshotRepository.save(new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                1000, 500, 100, 50000, 3.5, now
        ));

        FireWatchSummaryDto result = dashboardUseCase.getFireWatchSummary(userId);

        assertEquals(1, result.monitoredCount());
        assertEquals(1, result.normalCount());
        assertEquals(0, result.cautionCount());
        assertEquals(0, result.criticalCount());
        assertTrue(result.alertPosts().isEmpty());
    }

    @Test
    void getFireWatchSummary_classifiesCautionWhenDeltaAboveTwo() {
        Instant now = Instant.now();
        UUID accountId = UUID.randomUUID();

        snsAccountRepository.save(new SnsAccount(
                accountId, userId, SnsPlatform.TWITTER, "@user", "User",
                "token", "refresh", now.plusSeconds(3600), now, now
        ));

        impressionSnapshotRepository.save(new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                1000, 500, 100, 50000, 3.0, now.minusSeconds(3600)
        ));
        impressionSnapshotRepository.save(new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                1000, 500, 100, 50000, 6.0, now
        ));

        FireWatchSummaryDto result = dashboardUseCase.getFireWatchSummary(userId);

        assertEquals(1, result.monitoredCount());
        assertEquals(1, result.cautionCount());
        assertEquals(1, result.alertPosts().size());
        assertEquals("caution", result.alertPosts().get(0).severity());
    }

    @Test
    void getFireWatchSummary_classifiesCriticalWhenDeltaAboveFive() {
        Instant now = Instant.now();
        UUID accountId = UUID.randomUUID();

        snsAccountRepository.save(new SnsAccount(
                accountId, userId, SnsPlatform.TWITTER, "@user", "User",
                "token", "refresh", now.plusSeconds(3600), now, now
        ));

        impressionSnapshotRepository.save(new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                1000, 500, 100, 50000, 1.0, now.minusSeconds(3600)
        ));
        impressionSnapshotRepository.save(new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                1000, 500, 100, 50000, 8.0, now
        ));

        FireWatchSummaryDto result = dashboardUseCase.getFireWatchSummary(userId);

        assertEquals(1, result.criticalCount());
        assertEquals(1, result.alertPosts().size());
        assertEquals("critical", result.alertPosts().get(0).severity());
    }

    @Test
    void getFireWatchSummary_returnsZerosForUserWithNoAccounts() {
        FireWatchSummaryDto result = dashboardUseCase.getFireWatchSummary(userId);

        assertEquals(0, result.monitoredCount());
        assertEquals(0, result.normalCount());
        assertEquals(0, result.cautionCount());
        assertEquals(0, result.criticalCount());
        assertTrue(result.alertPosts().isEmpty());
    }

    private Post createPost(UUID userId, String content, PostStatus status, Instant now) {
        return new Post(UUID.randomUUID(), userId, content, List.of(), status, null, now, now);
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
            return new EnumMap<>(SnsPlatform.class);
        }
    }

    private static class InMemoryPostTargetRepository implements PostTargetRepository {

        private final Map<UUID, PostTarget> store = new HashMap<>();
        private UUID associatedUserId;

        void setUserIdForAll(UUID userId) {
            this.associatedUserId = userId;
        }

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

        @Override
        public Map<String, Long> countByStatusForUser(UUID userId) {
            if (associatedUserId == null || !associatedUserId.equals(userId)) {
                return Map.of();
            }
            Map<String, Long> counts = new HashMap<>();
            for (PostTarget target : store.values()) {
                counts.merge(target.getStatus().name(), 1L, Long::sum);
            }
            return counts;
        }

        @Override
        public List<PostTarget> findByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to) {
            if (associatedUserId == null || !associatedUserId.equals(userId)) {
                return List.of();
            }
            return store.values().stream()
                    .filter(target -> {
                        Instant created = target.getCreatedAt();
                        return created != null
                                && !created.isBefore(from)
                                && created.isBefore(to);
                    })
                    .toList();
        }

        @Override
        public List<PostTarget> findByUserIdAndPlatformAndCreatedAtBetween(UUID userId, SnsPlatform platform, Instant from, Instant to) {
            if (associatedUserId == null || !associatedUserId.equals(userId)) {
                return List.of();
            }
            return store.values().stream()
                    .filter(target -> target.getPlatform() == platform)
                    .filter(target -> {
                        Instant created = target.getCreatedAt();
                        return created != null
                                && !created.isBefore(from)
                                && created.isBefore(to);
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

    private static class InMemoryAuditLogRepository implements AuditLogRepository {

        private final List<AuditLog> store = new ArrayList<>();

        @Override
        public void save(AuditLog auditLog) {
            store.add(auditLog);
        }

        @Override
        public List<AuditLog> findByUserId(UUID userId, int offset, int limit) {
            return store.stream()
                    .filter(log -> userId.equals(log.getUserId()))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<AuditLog> findByRequestPath(String pathPrefix, int offset, int limit) {
            return store.stream()
                    .filter(log -> log.getRequestPath() != null && log.getRequestPath().startsWith(pathPrefix))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<AuditLog> findByCreatedAtBetween(Instant from, Instant to, int offset, int limit) {
            return store.stream()
                    .filter(log -> !log.getCreatedAt().isBefore(from) && log.getCreatedAt().isBefore(to))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<AuditLog> findByCategory(String category, int offset, int limit) {
            return store.stream()
                    .filter(log -> category.equals(log.getCategory()))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countAll() {
            return store.size();
        }

        @Override
        public List<AuditLog> findRecent(int limit) {
            return store.stream()
                    .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public Map<String, Long> countByResponseStatusRange() {
            long total = store.size();
            long success = store.stream().filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300).count();
            long warn = store.stream().filter(l -> l.getResponseStatus() >= 400 && l.getResponseStatus() < 500).count();
            long error = store.stream().filter(l -> l.getResponseStatus() >= 500).count();
            return Map.of("total", total, "success", success, "warn", warn, "error", error);
        }
    }

    private static class InMemoryImpressionSnapshotRepository implements ImpressionSnapshotRepository {

        private final Map<UUID, ImpressionSnapshot> store = new HashMap<>();

        @Override
        public List<ImpressionSnapshot> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
            return store.values().stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .skip(offset)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ImpressionSnapshot> findBySnsAccountIdAndDateRange(UUID snsAccountId, Instant from, Instant to) {
            return store.values().stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .filter(s -> !s.getSnapshotAt().isBefore(from) && s.getSnapshotAt().isBefore(to))
                    .toList();
        }

        @Override
        public ImpressionSnapshot save(ImpressionSnapshot snapshot) {
            store.put(snapshot.getId(), snapshot);
            return snapshot;
        }

        @Override
        public Optional<ImpressionSnapshot> getLatestBySnsAccountId(UUID snsAccountId) {
            return store.values().stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .max(Comparator.comparing(ImpressionSnapshot::getSnapshotAt));
        }

        @Override
        public List<ImpressionSnapshot> findRecentByAccountIds(List<UUID> snsAccountIds, int limit) {
            return store.values().stream()
                    .filter(s -> snsAccountIds.contains(s.getSnsAccountId()))
                    .sorted(Comparator.comparing(ImpressionSnapshot::getSnapshotAt).reversed())
                    .limit(limit)
                    .toList();
        }
    }
}
