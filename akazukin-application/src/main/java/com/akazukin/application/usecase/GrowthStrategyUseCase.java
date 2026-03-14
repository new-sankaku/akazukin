package com.akazukin.application.usecase;

import com.akazukin.application.dto.GrowthAdviceDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.ImpressionSnapshotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class GrowthStrategyUseCase {

    private static final Logger LOG = Logger.getLogger(GrowthStrategyUseCase.class.getName());

    private final AgentOrchestrator orchestrator;
    private final ImpressionSnapshotRepository impressionRepo;

    @Inject
    public GrowthStrategyUseCase(AgentOrchestrator orchestrator,
                                  ImpressionSnapshotRepository impressionRepo) {
        this.orchestrator = orchestrator;
        this.impressionRepo = impressionRepo;
    }

    public GrowthAdviceDto getGrowthAdvice(UUID userId, UUID snsAccountId) {
        long perfStart = System.nanoTime();
        try {
            List<ImpressionSnapshot> snapshots = impressionRepo.findBySnsAccountId(snsAccountId, 0, 30);
            if (snapshots.isEmpty()) {
                throw new DomainException("NO_DATA", "No impression data available for this account");
            }

            ImpressionSnapshot latest = snapshots.get(0);
            String platform = latest.getPlatform().name();
            String currentFollowers = String.valueOf(latest.getFollowersCount());

            StringBuilder context = new StringBuilder();
            context.append("Platform: ").append(platform).append("\n");
            context.append("Current followers: ").append(latest.getFollowersCount()).append("\n");
            context.append("Current following: ").append(latest.getFollowingCount()).append("\n");
            context.append("Total posts: ").append(latest.getPostCount()).append("\n");
            context.append("Total impressions: ").append(latest.getImpressionsCount()).append("\n");
            context.append("Engagement rate: ").append(String.format("%.2f%%", latest.getEngagementRate() * 100)).append("\n");
            context.append("\nHistorical data (last ").append(snapshots.size()).append(" snapshots):\n");

            for (ImpressionSnapshot snapshot : snapshots) {
                context.append("  [").append(snapshot.getSnapshotAt()).append("] ");
                context.append("followers=").append(snapshot.getFollowersCount());
                context.append(", impressions=").append(snapshot.getImpressionsCount());
                context.append(", engagement=").append(String.format("%.2f%%", snapshot.getEngagementRate() * 100));
                context.append("\n");
            }

            context.append("\nProvide a growth strategy with projected growth estimate.");

            LOG.log(Level.INFO, "Requesting growth advice for user {0}, account {1}",
                    new Object[]{userId, snsAccountId});

            AgentTask analysisTask = orchestrator.submitTask(userId, AgentType.ANALYST, context.toString());

            String projectedGrowth = estimateProjectedGrowth(snapshots);

            return new GrowthAdviceDto(
                    platform,
                    analysisTask.getOutput(),
                    currentFollowers,
                    projectedGrowth
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"GrowthStrategyUseCase.getGrowthAdvice", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"GrowthStrategyUseCase.getGrowthAdvice", perfMs});
            }
        }
    }

    private String estimateProjectedGrowth(List<ImpressionSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return "Insufficient data for projection";
        }

        ImpressionSnapshot latest = snapshots.get(0);
        ImpressionSnapshot oldest = snapshots.get(snapshots.size() - 1);

        int followerChange = latest.getFollowersCount() - oldest.getFollowersCount();
        long daysBetween = java.time.Duration.between(oldest.getSnapshotAt(), latest.getSnapshotAt()).toDays();

        if (daysBetween <= 0) {
            return "Insufficient time range for projection";
        }

        double dailyGrowthRate = (double) followerChange / daysBetween;
        int projected30Days = (int) (latest.getFollowersCount() + dailyGrowthRate * 30);

        return String.format("+%d followers/day, projected 30-day: %d", (int) dailyGrowthRate, projected30Days);
    }
}
