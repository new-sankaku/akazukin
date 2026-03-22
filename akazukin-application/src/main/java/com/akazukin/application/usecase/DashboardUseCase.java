package com.akazukin.application.usecase;

import com.akazukin.application.dto.AuditLogEntryDto;
import com.akazukin.application.dto.DailyRateDto;
import com.akazukin.application.dto.FireWatchPostDto;
import com.akazukin.application.dto.FireWatchSummaryDto;
import com.akazukin.application.dto.PlatformSuccessRateDto;
import com.akazukin.application.dto.PodMonitorDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.PostTargetDto;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardUseCase {

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final ImpressionSnapshotRepository impressionSnapshotRepository;

    @Inject
    public DashboardUseCase(PostRepository postRepository,
                            PostTargetRepository postTargetRepository,
                            SnsAccountRepository snsAccountRepository,
                            AuditLogRepository auditLogRepository,
                            ImpressionSnapshotRepository impressionSnapshotRepository) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.auditLogRepository = auditLogRepository;
        this.impressionSnapshotRepository = impressionSnapshotRepository;
    }

    public DashboardSummary getSummary(UUID userId) {
        long totalPosts = postRepository.countByUserId(userId);
        long publishedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);
        long failedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.FAILED);
        long scheduledPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.SCHEDULED);

        List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
        int connectedAccounts = accounts.size();

        Map<SnsPlatform, Integer> postCountByPlatform = calculatePostCountByPlatform(userId);

        return new DashboardSummary(
                (int) totalPosts,
                (int) publishedPosts,
                (int) failedPosts,
                (int) scheduledPosts,
                connectedAccounts,
                postCountByPlatform,
                List.of()
        );
    }

    public List<PostResponseDto> getRecentPosts(UUID userId, int limit) {
        List<Post> posts = postRepository.findByUserId(userId, 0, limit);

        List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<PostTarget> allTargets = postTargetRepository.findByPostIds(postIds);
        Map<UUID, List<PostTarget>> targetsByPostId = allTargets.stream()
                .collect(Collectors.groupingBy(PostTarget::getPostId));

        return posts.stream()
                .map(post -> {
                    List<PostTarget> targets = targetsByPostId.getOrDefault(
                            post.getId(), List.of());
                    List<PostTargetDto> targetDtos = targets.stream()
                            .map(target -> new PostTargetDto(
                                    target.getId(),
                                    target.getPlatform().name(),
                                    target.getStatus().name(),
                                    target.getPlatformPostId(),
                                    target.getErrorMessage(),
                                    target.getPublishedAt()
                            ))
                            .toList();

                    return new PostResponseDto(
                            post.getId(),
                            post.getContent(),
                            post.getStatus().name(),
                            post.getScheduledAt(),
                            post.getCreatedAt(),
                            targetDtos
                    );
                })
                .toList();
    }

    public Map<String, Object> getTimelineSummary(UUID userId) {
        List<PostResponseDto> recentPosts = getRecentPosts(userId, 20);
        DashboardSummary summary = getSummary(userId);

        return Map.of(
                "summary", summary,
                "recentPosts", recentPosts
        );
    }

    public PodMonitorDto getPodMonitorLog(int limit) {
        List<AuditLog> logs = auditLogRepository.findRecent(limit);
        Map<String, Long> statusCounts = auditLogRepository.countByResponseStatusRange();

        List<AuditLogEntryDto> entries = logs.stream()
                .map(log -> {
                    String timestamp = DateTimeFormatter.ofPattern("HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(log.getCreatedAt());

                    String level = resolveLogLevel(log.getResponseStatus());

                    String platform = extractPlatformFromPath(log.getRequestPath());

                    String message = log.getHttpMethod() + " " + log.getRequestPath()
                            + " " + log.getResponseStatus()
                            + " (" + log.getDurationMs() + "ms)";

                    return new AuditLogEntryDto(timestamp, level, platform, message);
                })
                .toList();

        return new PodMonitorDto(
                statusCounts.getOrDefault("total", 0L),
                statusCounts.getOrDefault("success", 0L),
                statusCounts.getOrDefault("warn", 0L),
                statusCounts.getOrDefault("error", 0L),
                entries
        );
    }

    public QueueStatusDto getQueueStatus(UUID userId) {
        Map<String, Long> statusCounts = postTargetRepository.countByStatusForUser(userId);

        long pending = statusCounts.getOrDefault(PostStatus.SCHEDULED.name(), 0L)
                + statusCounts.getOrDefault(PostStatus.PENDING_APPROVAL.name(), 0L)
                + statusCounts.getOrDefault(PostStatus.APPROVED.name(), 0L);
        long processing = statusCounts.getOrDefault(PostStatus.PUBLISHING.name(), 0L);
        long completed = statusCounts.getOrDefault(PostStatus.PUBLISHED.name(), 0L);
        long failed = statusCounts.getOrDefault(PostStatus.FAILED.name(), 0L);

        return new QueueStatusDto(pending, processing, completed, failed);
    }

    public List<TrendWordDto> getTrendWords(UUID userId) {
        List<Post> recentPosts = postRepository.findByUserId(userId, 0, 100);

        Map<String, Integer> wordFrequency = new HashMap<>();
        for (Post post : recentPosts) {
            String content = post.getContent();
            if (content == null) continue;
            extractHashtags(content, wordFrequency);
            extractKeywords(content, wordFrequency);
        }

        List<Map.Entry<String, Integer>> sorted = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .toList();

        int maxVolume = sorted.isEmpty() ? 1 : sorted.get(0).getValue();

        List<TrendWordDto> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            String affinity = calculateAffinity(entry.getValue(), maxVolume);
            result.add(new TrendWordDto(
                    i + 1,
                    entry.getKey(),
                    entry.getValue(),
                    affinity
            ));
        }
        return result;
    }

    public List<PlatformSuccessRateDto> getPlatformTimeline(UUID userId, int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);

        List<PostTarget> targets = postTargetRepository.findByUserIdAndCreatedAtBetween(userId, from, now);

        Map<SnsPlatform, List<PostTarget>> byPlatform = targets.stream()
                .collect(Collectors.groupingBy(PostTarget::getPlatform));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d");

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
    }

    public FireWatchSummaryDto getFireWatchSummary(UUID userId) {
        List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
        List<UUID> accountIds = accounts.stream().map(SnsAccount::getId).toList();

        List<ImpressionSnapshot> snapshots = impressionSnapshotRepository.findRecentByAccountIds(accountIds, 200);

        Map<UUID, List<ImpressionSnapshot>> byAccount = snapshots.stream()
                .collect(Collectors.groupingBy(ImpressionSnapshot::getSnsAccountId));

        long monitoredCount = 0;
        long normalCount = 0;
        long cautionCount = 0;
        long criticalCount = 0;
        List<FireWatchPostDto> alertPosts = new ArrayList<>();

        for (Map.Entry<UUID, List<ImpressionSnapshot>> entry : byAccount.entrySet()) {
            List<ImpressionSnapshot> accountSnapshots = entry.getValue().stream()
                    .sorted(Comparator.comparing(ImpressionSnapshot::getSnapshotAt).reversed())
                    .toList();

            monitoredCount++;

            if (accountSnapshots.size() < 2) {
                normalCount++;
                continue;
            }

            ImpressionSnapshot latest = accountSnapshots.get(0);
            ImpressionSnapshot previous = accountSnapshots.get(1);

            double delta = latest.getEngagementRate() - previous.getEngagementRate();
            double absDelta = Math.abs(delta);

            String severity;
            if (absDelta >= 5.0) {
                severity = "critical";
                criticalCount++;
            } else if (absDelta >= 2.0) {
                severity = "caution";
                cautionCount++;
            } else {
                severity = "normal";
                normalCount++;
            }

            if (!"normal".equals(severity)) {
                alertPosts.add(new FireWatchPostDto(
                        latest.getId(),
                        latest.getPlatform().name(),
                        latest.getPlatform().name() + " " + latest.getSnsAccountId(),
                        latest.getSnapshotAt(),
                        severity,
                        latest.getEngagementRate(),
                        delta,
                        latest.getImpressionsCount()
                ));
            }
        }

        alertPosts.sort(Comparator.comparing(FireWatchPostDto::severity)
                .thenComparing(Comparator.comparing(FireWatchPostDto::publishedAt).reversed()));

        return new FireWatchSummaryDto(
                monitoredCount,
                normalCount,
                cautionCount,
                criticalCount,
                alertPosts
        );
    }

    private Map<SnsPlatform, Integer> calculatePostCountByPlatform(UUID userId) {
        Map<SnsPlatform, Long> counts = postRepository.countByUserIdGroupByPlatform(userId);
        return counts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()
                ));
    }

    private String resolveLogLevel(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "ok";
        if (statusCode >= 300 && statusCode < 400) return "info";
        if (statusCode >= 400 && statusCode < 500) return "warn";
        return "err";
    }

    private String extractPlatformFromPath(String path) {
        if (path == null) return "";
        String lower = path.toLowerCase();
        if (lower.contains("twitter")) return "twitter";
        if (lower.contains("bluesky")) return "bluesky";
        if (lower.contains("mastodon")) return "mastodon";
        if (lower.contains("threads")) return "threads";
        if (lower.contains("instagram")) return "instagram";
        if (lower.contains("reddit")) return "reddit";
        if (lower.contains("telegram")) return "telegram";
        return "";
    }

    private void extractHashtags(String content, Map<String, Integer> frequency) {
        int idx = 0;
        while (idx < content.length()) {
            int hashIdx = content.indexOf('#', idx);
            if (hashIdx < 0) break;
            int end = hashIdx + 1;
            while (end < content.length() && !Character.isWhitespace(content.charAt(end))) {
                end++;
            }
            if (end > hashIdx + 1) {
                String tag = content.substring(hashIdx, end);
                frequency.merge(tag, 1, Integer::sum);
            }
            idx = end;
        }
    }

    private void extractKeywords(String content, Map<String, Integer> frequency) {
        String cleaned = content.replaceAll("#\\S+", "").replaceAll("https?://\\S+", "");
        String[] tokens = cleaned.split("[\\s、。！？「」（）\\[\\]\\{\\}\\n\\r]+");
        for (String token : tokens) {
            if (token.length() >= 3 && token.length() <= 20) {
                frequency.merge(token, 1, Integer::sum);
            }
        }
    }

    private String calculateAffinity(int volume, int maxVolume) {
        double ratio = (double) volume / maxVolume;
        if (ratio >= 0.7) return "high";
        if (ratio >= 0.4) return "medium";
        return "low";
    }
}
