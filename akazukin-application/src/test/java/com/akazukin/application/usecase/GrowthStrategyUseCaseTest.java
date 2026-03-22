package com.akazukin.application.usecase;

import com.akazukin.application.dto.GrowthAdviceDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.ImpressionSnapshotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthStrategyUseCaseTest {

    private StubAgentOrchestrator orchestrator;
    private InMemoryImpressionSnapshotRepository impressionRepo;
    private GrowthStrategyUseCase growthStrategyUseCase;

    private UUID userId;
    private UUID snsAccountId;

    @BeforeEach
    void setUp() {
        orchestrator = new StubAgentOrchestrator();
        impressionRepo = new InMemoryImpressionSnapshotRepository();
        growthStrategyUseCase = new GrowthStrategyUseCase(orchestrator, impressionRepo);
        userId = UUID.randomUUID();
        snsAccountId = UUID.randomUUID();
    }

    @Test
    void getGrowthAdvice_returnsAdviceWithProjection() {
        Instant now = Instant.now();
        Instant tenDaysAgo = now.minus(10, ChronoUnit.DAYS);

        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1100, 200, 50, 5000L, 0.05, now));
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1000, 190, 40, 4000L, 0.04, tenDaysAgo));

        GrowthAdviceDto result = growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId);

        assertNotNull(result);
        assertEquals("TWITTER", result.platform());
        assertEquals("1100", result.currentFollowers());
        assertNotNull(result.advice());
        assertNotNull(result.projectedGrowth());
        assertTrue(result.projectedGrowth().contains("followers/day"));
    }

    @Test
    void getGrowthAdvice_returnsInsufficientDataForSingleSnapshot() {
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1000, 200, 50, 5000L, 0.05, Instant.now()));

        GrowthAdviceDto result = growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId);

        assertEquals("Insufficient data for projection", result.projectedGrowth());
    }

    @Test
    void getGrowthAdvice_throwsNoDataWhenNoSnapshots() {
        DomainException exception = assertThrows(DomainException.class,
                () -> growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId));
        assertEquals("NO_DATA", exception.getErrorCode());
    }

    @Test
    void getGrowthAdvice_throwsNoDataForUnknownAccount() {
        UUID unknownAccountId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> growthStrategyUseCase.getGrowthAdvice(userId, unknownAccountId));
        assertEquals("NO_DATA", exception.getErrorCode());
    }

    @Test
    void getGrowthAdvice_submitsAnalystTask() {
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1000, 200, 50, 5000L, 0.05, Instant.now()));

        growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId);

        assertEquals(1, orchestrator.submittedTasks.size());
        assertEquals(AgentType.ANALYST, orchestrator.submittedTasks.get(0).getAgentType());
    }

    @Test
    void getGrowthAdvice_usesPlatformFromSnapshot() {
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.BLUESKY,
                500, 100, 30, 2000L, 0.03, Instant.now()));

        GrowthAdviceDto result = growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId);

        assertEquals("BLUESKY", result.platform());
    }

    @Test
    void getGrowthAdvice_returnsInsufficientTimeRangeForSameDay() {
        Instant now = Instant.now();
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1100, 200, 50, 5000L, 0.05, now));
        impressionRepo.save(new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1000, 190, 40, 4000L, 0.04, now));

        GrowthAdviceDto result = growthStrategyUseCase.getGrowthAdvice(userId, snsAccountId);

        assertEquals("Insufficient time range for projection", result.projectedGrowth());
    }

    private static class StubAgentOrchestrator implements AgentOrchestrator {

        final List<AgentTask> submittedTasks = new ArrayList<>();

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input) {
            return submitTask(userId, agentType, input, null);
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId) {
            Instant now = Instant.now();
            AgentTask task = new AgentTask(
                    UUID.randomUUID(), userId, agentType, input,
                    "growth-advice-output",
                    "COMPLETED", parentTaskId, now, now
            );
            submittedTasks.add(task);
            return task;
        }

        @Override
        public AgentTask getTaskResult(UUID taskId) {
            return submittedTasks.stream()
                    .filter(t -> t.getId().equals(taskId))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static class InMemoryImpressionSnapshotRepository implements ImpressionSnapshotRepository {

        private final Map<UUID, ImpressionSnapshot> store = new HashMap<>();

        @Override
        public List<ImpressionSnapshot> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
            List<ImpressionSnapshot> filtered = store.values().stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .sorted(Comparator.comparing(ImpressionSnapshot::getSnapshotAt).reversed())
                    .toList();
            int end = Math.min(offset + limit, filtered.size());
            if (offset >= filtered.size()) {
                return List.of();
            }
            return new ArrayList<>(filtered.subList(offset, end));
        }

        @Override
        public List<ImpressionSnapshot> findBySnsAccountIdAndDateRange(UUID snsAccountId, Instant from, Instant to) {
            return store.values().stream()
                    .filter(s -> s.getSnsAccountId().equals(snsAccountId))
                    .filter(s -> !s.getSnapshotAt().isBefore(from) && !s.getSnapshotAt().isAfter(to))
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
