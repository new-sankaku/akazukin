package com.akazukin.application.usecase;

import com.akazukin.application.dto.AccountStatsDto;
import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.DashboardSummary;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAnalyticsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsUseCase {

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final Map<SnsPlatform, SnsAnalyticsAdapter> analyticsAdapters;

    @Inject
    public AnalyticsUseCase(PostRepository postRepository,
                            PostTargetRepository postTargetRepository,
                            SnsAccountRepository snsAccountRepository,
                            Instance<SnsAnalyticsAdapter> analyticsAdapterInstances) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.analyticsAdapters = new EnumMap<>(SnsPlatform.class);
        for (SnsAnalyticsAdapter adapter : analyticsAdapterInstances) {
            this.analyticsAdapters.put(adapter.platform(), adapter);
        }
    }

    public DashboardSummary getDashboardSummary(UUID userId) {
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
    }

    public AnalyticsResponseDto getAnalytics(UUID userId) {
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
    }

    public List<SnsPostStats> getPostAnalytics(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        List<PostTarget> targets = postTargetRepository.findByPostId(postId);
        List<SnsPostStats> statsList = new ArrayList<>();

        for (PostTarget target : targets) {
            if (target.getPlatformPostId() == null) {
                continue;
            }

            SnsAnalyticsAdapter adapter = analyticsAdapters.get(target.getPlatform());
            if (adapter == null) {
                continue;
            }

            SnsAccount account = snsAccountRepository.findById(target.getSnsAccountId())
                    .orElse(null);
            if (account == null) {
                continue;
            }

            Optional<SnsPostStats> stats = adapter.getPostStats(
                    account.getAccessToken(),
                    target.getPlatformPostId()
            );
            stats.ifPresent(statsList::add);
        }

        return statsList;
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
        List<AccountStats> statsList = new ArrayList<>();

        for (SnsAccount account : accounts) {
            SnsAnalyticsAdapter adapter = analyticsAdapters.get(account.getPlatform());
            if (adapter == null) {
                statsList.add(new AccountStats(
                        account.getPlatform(),
                        account.getAccountIdentifier(),
                        0,
                        0,
                        0,
                        Instant.now()
                ));
                continue;
            }

            Optional<AccountStats> stats = adapter.getAccountStats(account.getAccessToken());
            statsList.add(stats.orElse(new AccountStats(
                    account.getPlatform(),
                    account.getAccountIdentifier(),
                    0,
                    0,
                    0,
                    Instant.now()
            )));
        }

        return statsList;
    }
}
