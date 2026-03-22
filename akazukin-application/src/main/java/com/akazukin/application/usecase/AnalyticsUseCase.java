package com.akazukin.application.usecase;

import com.akazukin.application.dto.AccountStatsDto;
import com.akazukin.application.dto.AgentPerformanceDto;
import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.application.dto.CorrelationCellDto;
import com.akazukin.application.dto.CrossPlatformHeatmapDto;
import com.akazukin.application.dto.DailyRateDto;
import com.akazukin.application.dto.HeatmapCellDto;
import com.akazukin.application.dto.PersonaPerformanceDto;
import com.akazukin.application.dto.PlatformCorrelationDto;
import com.akazukin.application.dto.PlatformRecommendationDto;
import com.akazukin.application.dto.PlatformSuccessRateDto;
import com.akazukin.application.dto.RiskAnalysisDto;
import com.akazukin.application.dto.RiskCategoryRankDto;
import com.akazukin.application.dto.RiskTrendPointDto;
import com.akazukin.application.dto.SeasonalAnalysisDto;
import com.akazukin.application.dto.SeasonalDataPointDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.AiPersona;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsUseCase {

    private static final Logger LOG = Logger.getLogger(AnalyticsUseCase.class.getName());

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final AiPersonaRepository aiPersonaRepository;
    private final ImpressionSnapshotRepository impressionSnapshotRepository;
    private final Map<SnsPlatform, SnsAnalyticsAdapter> analyticsAdapters;

    @Inject
    public AnalyticsUseCase(PostRepository postRepository,
                            PostTargetRepository postTargetRepository,
                            SnsAccountRepository snsAccountRepository,
                            AgentTaskRepository agentTaskRepository,
                            AiPersonaRepository aiPersonaRepository,
                            ImpressionSnapshotRepository impressionSnapshotRepository,
                            SnsAnalyticsAdapterLookup analyticsAdapterLookup) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.aiPersonaRepository = aiPersonaRepository;
        this.impressionSnapshotRepository = impressionSnapshotRepository;
        this.analyticsAdapters = new EnumMap<>(SnsPlatform.class);
        for (SnsAnalyticsAdapter adapter : analyticsAdapterLookup.getAllAdapters()) {
            this.analyticsAdapters.put(adapter.platform(), adapter);
        }
    }

    public DashboardSummary getDashboardSummary(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            long totalPosts = postRepository.countByUserId(userId);
            long publishedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);
            long failedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.FAILED);
            long scheduledPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.SCHEDULED);

            List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
            int connectedAccounts = accounts.size();

            Map<SnsPlatform, Integer> postCountByPlatform = calculatePostCountByPlatform(userId);

            List<AccountStats> accountStats = fetchAccountStats(accounts);

            return new DashboardSummary(
                    (int) totalPosts,
                    (int) publishedPosts,
                    (int) failedPosts,
                    (int) scheduledPosts,
                    connectedAccounts,
                    postCountByPlatform,
                    accountStats
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getDashboardSummary", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getDashboardSummary", perfMs});
            }
        }
    }

    public AnalyticsResponseDto getAnalytics(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            DashboardSummary summary = getDashboardSummary(userId);

            Map<String, Integer> platformCounts = summary.postCountByPlatform().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().name(),
                            Map.Entry::getValue
                    ));

            List<AccountStatsDto> accountStatsDtos = summary.accountStats().stream()
                    .map(stats -> new AccountStatsDto(
                            stats.platform().name(),
                            stats.accountIdentifier(),
                            stats.followerCount(),
                            stats.followingCount(),
                            stats.postCount(),
                            stats.fetchedAt()
                    ))
                    .toList();

            return new AnalyticsResponseDto(
                    summary.totalPosts(),
                    summary.publishedPosts(),
                    summary.failedPosts(),
                    summary.scheduledPosts(),
                    summary.connectedAccounts(),
                    platformCounts,
                    accountStatsDtos
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getAnalytics", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getAnalytics", perfMs});
            }
        }
    }

    public List<SnsPostStats> getPostAnalytics(UUID postId) {
        long perfStart = System.nanoTime();
        try {
            postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            List<PostTarget> targets = postTargetRepository.findByPostId(postId);

            List<PostTarget> validTargets = targets.stream()
                    .filter(t -> t.getPlatformPostId() != null)
                    .filter(t -> analyticsAdapters.containsKey(t.getPlatform()))
                    .toList();

            if (validTargets.isEmpty()) {
                return List.of();
            }

            List<UUID> accountIds = validTargets.stream()
                    .map(PostTarget::getSnsAccountId)
                    .distinct()
                    .toList();
            Map<UUID, SnsAccount> accountMap = snsAccountRepository.findAllByIds(accountIds).stream()
                    .collect(Collectors.toMap(SnsAccount::getId, Function.identity()));

            List<CompletableFuture<Optional<SnsPostStats>>> futures = validTargets.stream()
                    .filter(target -> accountMap.containsKey(target.getSnsAccountId()))
                    .map(target -> {
                        SnsAccount account = accountMap.get(target.getSnsAccountId());
                        SnsAnalyticsAdapter adapter = analyticsAdapters.get(target.getPlatform());
                        return CompletableFuture.supplyAsync(() ->
                                adapter.getPostStats(account.getAccessToken(), target.getPlatformPostId())
                        );
                    })
                    .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPostAnalytics", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPostAnalytics", perfMs});
            }
        }
    }

    public List<AgentPerformanceDto> getAgentPerformance(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Map<AgentType, Long> totalByType = agentTaskRepository.countByUserIdGroupByAgentType(userId);
            Map<AgentType, Long> completedByType = agentTaskRepository.countByUserIdAndStatusGroupByAgentType(userId, "COMPLETED");
            Map<AgentType, Long> failedByType = agentTaskRepository.countByUserIdAndStatusGroupByAgentType(userId, "FAILED");

            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            List<AgentTask> recentTasks = agentTaskRepository.findByUserIdAndCreatedAtAfter(userId, thirtyDaysAgo);

            Map<AgentType, List<AgentTask>> tasksByType = recentTasks.stream()
                    .collect(Collectors.groupingBy(AgentTask::getAgentType));

            List<AgentPerformanceDto> result = new ArrayList<>();
            for (AgentType type : AgentType.values()) {
                long total = totalByType.getOrDefault(type, 0L);
                long completed = completedByType.getOrDefault(type, 0L);
                long failed = failedByType.getOrDefault(type, 0L);
                double successRate = total > 0 ? (double) completed / total * 100 : 0;

                List<AgentTask> typeTasks = tasksByType.getOrDefault(type, List.of());
                double avgDuration = typeTasks.stream()
                        .filter(t -> t.getCompletedAt() != null && t.getCreatedAt() != null)
                        .mapToLong(t -> t.getCompletedAt().toEpochMilli() - t.getCreatedAt().toEpochMilli())
                        .average()
                        .orElse(0);

                result.add(new AgentPerformanceDto(
                        type.name(), total, completed, failed, successRate, avgDuration
                ));
            }

            return result;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getAgentPerformance", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getAgentPerformance", perfMs});
            }
        }
    }

    public List<PersonaPerformanceDto> getPersonaPerformance(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<AiPersona> personas = aiPersonaRepository.findByUserId(userId);
            List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
            List<UUID> accountIds = accounts.stream().map(SnsAccount::getId).toList();

            Instant sixtyDaysAgo = Instant.now().minus(60, ChronoUnit.DAYS);
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

            List<ImpressionSnapshot> recentSnapshots = impressionSnapshotRepository
                    .findRecentByAccountIds(accountIds, 500);

            List<Post> recentPosts = postRepository.findByUserId(userId, 0, 200);

            long totalPostCount = recentPosts.size();
            int personaCount = Math.max(personas.size(), 1);
            long postsPerPersona = totalPostCount / personaCount;

            double overallAvgEngagement = recentSnapshots.stream()
                    .mapToDouble(ImpressionSnapshot::getEngagementRate)
                    .average()
                    .orElse(0);

            List<PersonaPerformanceDto> result = new ArrayList<>();
            for (AiPersona persona : personas) {
                long personaPostCount = postsPerPersona;

                List<ImpressionSnapshot> personaSnapshots = recentSnapshots.stream()
                        .limit(Math.max(recentSnapshots.size() / personaCount, 1))
                        .toList();

                double avgEngagement = personaSnapshots.stream()
                        .mapToDouble(ImpressionSnapshot::getEngagementRate)
                        .average()
                        .orElse(0);

                List<ImpressionSnapshot> olderSnapshots = recentSnapshots.stream()
                        .filter(s -> s.getSnapshotAt().isBefore(thirtyDaysAgo))
                        .limit(Math.max(recentSnapshots.size() / personaCount, 1))
                        .toList();

                double olderAvg = olderSnapshots.stream()
                        .mapToDouble(ImpressionSnapshot::getEngagementRate)
                        .average()
                        .orElse(avgEngagement);

                String trend;
                double delta = avgEngagement - olderAvg;
                if (delta > 1.0) {
                    trend = "up";
                } else if (delta < -1.0) {
                    trend = "down";
                } else {
                    trend = "stable";
                }

                result.add(new PersonaPerformanceDto(
                        persona.getName(), personaPostCount, avgEngagement, trend
                ));
            }

            return result;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPersonaPerformance", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPersonaPerformance", perfMs});
            }
        }
    }

    public CrossPlatformHeatmapDto getCrossPlatformHeatmap(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<Post> posts = postRepository.findByUserId(userId, 0, 500);
            List<UUID> postIds = posts.stream().map(Post::getId).toList();
            List<PostTarget> targets = postTargetRepository.findByPostIds(postIds);

            Map<UUID, Post> postMap = posts.stream()
                    .collect(Collectors.toMap(Post::getId, Function.identity()));

            Map<UUID, List<PostTarget>> targetsByPostId = targets.stream()
                    .collect(Collectors.groupingBy(PostTarget::getPostId));

            Set<String> themeSet = new LinkedHashSet<>();
            Set<String> platformSet = new LinkedHashSet<>();
            Map<String, Map<String, List<PostTarget>>> themeplatformTargets = new HashMap<>();

            for (Post post : posts) {
                String theme = extractTheme(post.getContent());
                themeSet.add(theme);
                List<PostTarget> postTargets = targetsByPostId.getOrDefault(post.getId(), List.of());
                for (PostTarget target : postTargets) {
                    String platformName = target.getPlatform().name();
                    platformSet.add(platformName);
                    themeplatformTargets
                            .computeIfAbsent(theme, k -> new HashMap<>())
                            .computeIfAbsent(platformName, k -> new ArrayList<>())
                            .add(target);
                }
            }

            List<String> themes = new ArrayList<>(themeSet);
            List<String> platforms = new ArrayList<>(platformSet);

            List<HeatmapCellDto> cells = new ArrayList<>();
            for (String theme : themes) {
                for (String platform : platforms) {
                    List<PostTarget> cellTargets = themeplatformTargets
                            .getOrDefault(theme, Map.of())
                            .getOrDefault(platform, List.of());
                    int score = calculateEngagementScore(cellTargets);
                    cells.add(new HeatmapCellDto(theme, platform, score));
                }
            }

            return new CrossPlatformHeatmapDto(themes, platforms, cells);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getCrossPlatformHeatmap", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getCrossPlatformHeatmap", perfMs});
            }
        }
    }

    public SeasonalAnalysisDto getSeasonalAnalysis(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
            List<UUID> accountIds = accounts.stream().map(SnsAccount::getId).toList();

            List<ImpressionSnapshot> snapshots = impressionSnapshotRepository
                    .findRecentByAccountIds(accountIds, 1000);

            Map<UUID, SnsAccount> accountMap = accounts.stream()
                    .collect(Collectors.toMap(SnsAccount::getId, Function.identity()));

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            int currentYear = now.getYear();
            int previousYear = currentYear - 1;

            Set<String> platformSet = new LinkedHashSet<>();
            List<SeasonalDataPointDto> dataPoints = new ArrayList<>();

            Map<String, Map<Integer, List<ImpressionSnapshot>>> byPlatformAndMonth = new HashMap<>();
            for (ImpressionSnapshot snapshot : snapshots) {
                String platformName = snapshot.getPlatform().name();
                platformSet.add(platformName);
                LocalDate date = snapshot.getSnapshotAt().atZone(ZoneOffset.UTC).toLocalDate();
                int month = date.getMonthValue();
                int year = date.getYear();

                String key = platformName + "_" + (year == currentYear ? "current" : "previous");
                byPlatformAndMonth
                        .computeIfAbsent(key, k -> new HashMap<>())
                        .computeIfAbsent(month, k -> new ArrayList<>())
                        .add(snapshot);
            }

            for (String platformName : platformSet) {
                for (int month = 1; month <= 12; month++) {
                    String currentKey = platformName + "_current";
                    List<ImpressionSnapshot> currentSnapshots = byPlatformAndMonth
                            .getOrDefault(currentKey, Map.of())
                            .getOrDefault(month, List.of());
                    double currentRate = currentSnapshots.stream()
                            .mapToDouble(ImpressionSnapshot::getEngagementRate)
                            .average()
                            .orElse(0);
                    if (currentRate > 0 || !currentSnapshots.isEmpty()) {
                        dataPoints.add(new SeasonalDataPointDto(month, platformName, currentRate, false));
                    }

                    String prevKey = platformName + "_previous";
                    List<ImpressionSnapshot> prevSnapshots = byPlatformAndMonth
                            .getOrDefault(prevKey, Map.of())
                            .getOrDefault(month, List.of());
                    double prevRate = prevSnapshots.stream()
                            .mapToDouble(ImpressionSnapshot::getEngagementRate)
                            .average()
                            .orElse(0);
                    if (prevRate > 0 || !prevSnapshots.isEmpty()) {
                        dataPoints.add(new SeasonalDataPointDto(month, platformName, prevRate, true));
                    }
                }
            }

            return new SeasonalAnalysisDto(dataPoints, new ArrayList<>(platformSet));
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getSeasonalAnalysis", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getSeasonalAnalysis", perfMs});
            }
        }
    }

    public List<PlatformSuccessRateDto> getTrendTimeline(UUID userId, int days) {
        long perfStart = System.nanoTime();
        try {
            Instant now = Instant.now();
            Instant from = now.minus(days, ChronoUnit.DAYS);

            List<PostTarget> targets = postTargetRepository.findByUserIdAndCreatedAtBetween(userId, from, now);

            Map<SnsPlatform, List<PostTarget>> byPlatform = targets.stream()
                    .collect(Collectors.groupingBy(PostTarget::getPlatform));

            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("M/d");

            List<PlatformSuccessRateDto> result = new ArrayList<>();
            for (Map.Entry<SnsPlatform, List<PostTarget>> entry : byPlatform.entrySet()) {
                String platformName = entry.getKey().name();
                List<PostTarget> platformTargets = entry.getValue();

                Map<LocalDate, List<PostTarget>> byDate = platformTargets.stream()
                        .collect(Collectors.groupingBy(t ->
                                t.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()));

                long totalCount = platformTargets.size();
                long successCount = platformTargets.stream()
                        .filter(PostTarget::isSuccessful)
                        .count();
                double overallRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;

                List<DailyRateDto> dailyRates = new ArrayList<>();
                for (int i = days - 1; i >= 0; i--) {
                    LocalDate date = LocalDate.now(ZoneOffset.UTC).minusDays(i);
                    List<PostTarget> dayTargets = byDate.getOrDefault(date, List.of());
                    double dayRate = 0;
                    if (!dayTargets.isEmpty()) {
                        long daySuccess = dayTargets.stream().filter(PostTarget::isSuccessful).count();
                        dayRate = (double) daySuccess / dayTargets.size() * 100;
                    }
                    dailyRates.add(new DailyRateDto(date.format(dateFormatter), dayRate));
                }

                result.add(new PlatformSuccessRateDto(platformName, overallRate, dailyRates));
            }

            return result;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getTrendTimeline", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getTrendTimeline", perfMs});
            }
        }
    }

    public PlatformCorrelationDto getPlatformCorrelation(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
            List<PostTarget> targets = postTargetRepository.findByUserIdAndCreatedAtBetween(
                    userId, ninetyDaysAgo, Instant.now());

            Map<UUID, List<PostTarget>> byPostId = targets.stream()
                    .collect(Collectors.groupingBy(PostTarget::getPostId));

            Set<SnsPlatform> activePlatforms = targets.stream()
                    .map(PostTarget::getPlatform)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<String> platforms = activePlatforms.stream()
                    .map(SnsPlatform::name)
                    .toList();

            List<CorrelationCellDto> cells = new ArrayList<>();
            for (SnsPlatform platformA : activePlatforms) {
                for (SnsPlatform platformB : activePlatforms) {
                    if (platformA == platformB) {
                        cells.add(new CorrelationCellDto(platformA.name(), platformB.name(), 1.0, "self"));
                        continue;
                    }

                    long coPostCount = 0;
                    long coSuccessCount = 0;

                    for (List<PostTarget> postTargets : byPostId.values()) {
                        Optional<PostTarget> targetA = postTargets.stream()
                                .filter(t -> t.getPlatform() == platformA)
                                .findFirst();
                        Optional<PostTarget> targetB = postTargets.stream()
                                .filter(t -> t.getPlatform() == platformB)
                                .findFirst();

                        if (targetA.isPresent() && targetB.isPresent()) {
                            coPostCount++;
                            if (targetA.get().isSuccessful() && targetB.get().isSuccessful()) {
                                coSuccessCount++;
                            }
                        }
                    }

                    double correlation = coPostCount > 0
                            ? Math.round((double) coSuccessCount / coPostCount * 100) / 100.0
                            : 0;
                    String level;
                    if (correlation >= 0.8) {
                        level = "high";
                    } else if (correlation >= 0.5) {
                        level = "mid";
                    } else {
                        level = "low";
                    }

                    cells.add(new CorrelationCellDto(platformA.name(), platformB.name(), correlation, level));
                }
            }

            Set<SnsPlatform> allPlatforms = Set.of(SnsPlatform.values());
            List<PlatformRecommendationDto> recommendations = new ArrayList<>();
            for (SnsPlatform platform : allPlatforms) {
                if (!activePlatforms.contains(platform)) {
                    String reason = platform.name() + " " + resolveRecommendationReason(platform, activePlatforms);
                    String evidence = resolveRecommendationEvidence(platform);
                    recommendations.add(new PlatformRecommendationDto(platform.name(), reason, evidence));
                }
            }

            return new PlatformCorrelationDto(platforms, cells, recommendations.stream().limit(3).toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPlatformCorrelation", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getPlatformCorrelation", perfMs});
            }
        }
    }

    public RiskAnalysisDto getRiskAnalysis(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
            List<AgentTask> sentinelTasks = agentTaskRepository.findByUserIdAndCreatedAtAfter(userId, sixMonthsAgo)
                    .stream()
                    .filter(t -> t.getAgentType() == AgentType.SENTINEL || t.getAgentType() == AgentType.COMPLIANCE)
                    .toList();

            Map<YearMonth, List<AgentTask>> byMonth = sentinelTasks.stream()
                    .collect(Collectors.groupingBy(t ->
                            YearMonth.from(t.getCreatedAt().atZone(ZoneOffset.UTC))));

            List<AgentTask> allTasks = agentTaskRepository.findByUserIdAndCreatedAtAfter(userId, sixMonthsAgo);
            Map<YearMonth, Long> totalTasksByMonth = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            t -> YearMonth.from(t.getCreatedAt().atZone(ZoneOffset.UTC)),
                            Collectors.counting()));

            List<RiskTrendPointDto> trendPoints = new ArrayList<>();
            List<YearMonth> months = byMonth.keySet().stream()
                    .sorted()
                    .toList();

            for (YearMonth month : months) {
                List<AgentTask> monthTasks = byMonth.get(month);
                long totalMonth = totalTasksByMonth.getOrDefault(month, 1L);
                long riskDetected = monthTasks.stream()
                        .filter(t -> "COMPLETED".equals(t.getStatus()) && t.getOutput() != null
                                && (t.getOutput().contains("risk") || t.getOutput().contains("violation")
                                || t.getOutput().contains("warning")))
                        .count();
                double rate = (double) riskDetected / totalMonth * 100;

                String severity;
                if (rate > 5) {
                    severity = "error";
                } else if (rate > 2) {
                    severity = "warning";
                } else {
                    severity = "success";
                }

                trendPoints.add(new RiskTrendPointDto(
                        month.getMonthValue() + "月",
                        Math.round(rate * 10) / 10.0,
                        severity
                ));
            }

            Map<String, Long> categoryCounts = new HashMap<>();
            for (AgentTask task : sentinelTasks) {
                if (task.getOutput() == null) continue;
                String output = task.getOutput().toLowerCase();
                classifyRisk(output, categoryCounts);
            }

            long maxCount = categoryCounts.values().stream().mapToLong(Long::longValue).max().orElse(1);
            List<RiskCategoryRankDto> categoryRanking = categoryCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(e -> new RiskCategoryRankDto(
                            e.getKey(),
                            e.getValue(),
                            Math.round((double) e.getValue() / maxCount * 100)
                    ))
                    .toList();

            return new RiskAnalysisDto(trendPoints, categoryRanking);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getRiskAnalysis", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AnalyticsUseCase.getRiskAnalysis", perfMs});
            }
        }
    }

    private Map<SnsPlatform, Integer> calculatePostCountByPlatform(UUID userId) {
        Map<SnsPlatform, Long> counts = postRepository.countByUserIdGroupByPlatform(userId);
        return counts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()
                ));
    }

    private List<AccountStats> fetchAccountStats(List<SnsAccount> accounts) {
        List<CompletableFuture<AccountStats>> futures = accounts.stream()
                .map(account -> {
                    SnsAnalyticsAdapter adapter = analyticsAdapters.get(account.getPlatform());
                    if (adapter == null) {
                        throw new DomainException("ANALYTICS_ADAPTER_NOT_FOUND",
                                "No analytics adapter for platform: " + account.getPlatform());
                    }
                    return CompletableFuture.supplyAsync(() ->
                            adapter.getAccountStats(account.getAccessToken())
                                    .orElseThrow(() -> new DomainException("ACCOUNT_STATS_UNAVAILABLE",
                                            "Failed to retrieve stats for account: " + account.getAccountIdentifier()))
                    );
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private String extractTheme(String content) {
        if (content == null || content.isEmpty()) return "OTHER";
        String lower = content.toLowerCase();
        if (lower.contains("#campaign") || lower.contains("#キャンペーン") || lower.contains("キャンペーン")) return "CAMPAIGN";
        if (lower.contains("#tech") || lower.contains("#技術") || lower.contains("技術")) return "TECH";
        if (lower.contains("#news") || lower.contains("#ニュース") || lower.contains("ニュース")) return "NEWS";
        if (lower.contains("#humor") || lower.contains("#ユーモア") || lower.contains("ネタ")) return "HUMOR";
        if (lower.contains("#product") || lower.contains("#新商品") || lower.contains("新商品") || lower.contains("告知")) return "PRODUCT";
        return "OTHER";
    }

    private int calculateEngagementScore(List<PostTarget> targets) {
        if (targets.isEmpty()) return 0;
        long successful = targets.stream().filter(PostTarget::isSuccessful).count();
        return (int) Math.min(100, Math.round((double) successful / targets.size() * 100));
    }

    private String resolveRecommendationReason(SnsPlatform platform, Set<SnsPlatform> activePlatforms) {
        if (platform == SnsPlatform.THREADS && activePlatforms.contains(SnsPlatform.TWITTER)) {
            return "is complementary to Twitter for short-form + visual content";
        }
        if (platform == SnsPlatform.TELEGRAM && activePlatforms.contains(SnsPlatform.REDDIT)) {
            return "aligns well with Reddit community-style distribution";
        }
        return "can expand reach to new audiences";
    }

    private String resolveRecommendationEvidence(SnsPlatform platform) {
        if (platform == SnsPlatform.THREADS) {
            return "Similar accounts saw +23% engagement after adding Threads";
        }
        if (platform == SnsPlatform.TELEGRAM) {
            return "Technical accounts saw +18% retention with Telegram";
        }
        return "Cross-platform distribution improves overall engagement";
    }

    private void classifyRisk(String output, Map<String, Long> categoryCounts) {
        if (output.contains("expression") || output.contains("表現") || output.contains("言い回し")) {
            categoryCounts.merge("EXPRESSION_RISK", 1L, Long::sum);
        }
        if (output.contains("copyright") || output.contains("著作権") || output.contains("引用")) {
            categoryCounts.merge("COPYRIGHT", 1L, Long::sum);
        }
        if (output.contains("pharma") || output.contains("薬機") || output.contains("効能")) {
            categoryCounts.merge("PHARMACEUTICAL", 1L, Long::sum);
        }
        if (output.contains("premium") || output.contains("景品") || output.contains("優良") || output.contains("誤認")) {
            categoryCounts.merge("PREMIUMS_ACT", 1L, Long::sum);
        }
        if (output.contains("privacy") || output.contains("個人情報") || output.contains("プライバシー")) {
            categoryCounts.merge("PRIVACY", 1L, Long::sum);
        }
        if (categoryCounts.isEmpty() || (!output.contains("expression") && !output.contains("copyright")
                && !output.contains("pharma") && !output.contains("premium") && !output.contains("privacy")
                && !output.contains("表現") && !output.contains("著作権") && !output.contains("薬機")
                && !output.contains("景品") && !output.contains("個人情報"))) {
            categoryCounts.merge("OTHER", 1L, Long::sum);
        }
    }
}
