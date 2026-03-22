package com.akazukin.application.usecase;

import com.akazukin.application.dto.ImpressionSnapshotDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.ImpressionSnapshotRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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

class ImpressionUseCaseTest {

    private InMemoryImpressionSnapshotRepository snapshotRepository;
    private ImpressionUseCase impressionUseCase;

    private UUID snsAccountId;

    @BeforeEach
    void setUp() {
        snapshotRepository = new InMemoryImpressionSnapshotRepository();
        impressionUseCase = new ImpressionUseCase(snapshotRepository);
        snsAccountId = UUID.randomUUID();
    }

    @Test
    void getTimeSeries_returnsSnapshotsWithinDateRange() {
        Instant base = Instant.parse("2026-01-15T00:00:00Z");
        ImpressionSnapshot s1 = createSnapshot(snsAccountId, base);
        ImpressionSnapshot s2 = createSnapshot(snsAccountId, base.plusSeconds(3600));
        ImpressionSnapshot s3 = createSnapshot(snsAccountId, base.plusSeconds(86400));
        snapshotRepository.save(s1);
        snapshotRepository.save(s2);
        snapshotRepository.save(s3);

        List<ImpressionSnapshotDto> result = impressionUseCase.getTimeSeries(
                snsAccountId, base, base.plusSeconds(7200));

        assertEquals(2, result.size());
    }

    @Test
    void getTimeSeries_returnsEmptyWhenNoSnapshotsInRange() {
        Instant base = Instant.parse("2026-01-15T00:00:00Z");
        ImpressionSnapshot s1 = createSnapshot(snsAccountId, base);
        snapshotRepository.save(s1);

        Instant rangeFrom = base.plusSeconds(86400);
        Instant rangeTo = base.plusSeconds(172800);
        List<ImpressionSnapshotDto> result = impressionUseCase.getTimeSeries(
                snsAccountId, rangeFrom, rangeTo);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimeSeries_returnsEmptyForUnknownAccount() {
        UUID unknownAccountId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        List<ImpressionSnapshotDto> result = impressionUseCase.getTimeSeries(
                unknownAccountId, from, to);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimeSeries_throwsInvalidInputWhenFromIsNull() {
        Instant to = Instant.now();

        DomainException exception = assertThrows(DomainException.class,
                () -> impressionUseCase.getTimeSeries(snsAccountId, null, to));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getTimeSeries_throwsInvalidInputWhenToIsNull() {
        Instant from = Instant.now();

        DomainException exception = assertThrows(DomainException.class,
                () -> impressionUseCase.getTimeSeries(snsAccountId, from, null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getTimeSeries_throwsInvalidInputWhenBothNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> impressionUseCase.getTimeSeries(snsAccountId, null, null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getTimeSeries_mapsDtoFieldsCorrectly() {
        Instant snapshotAt = Instant.parse("2026-03-01T12:00:00Z");
        ImpressionSnapshot snapshot = new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.TWITTER,
                1000, 200, 50, 5000L, 3.5, snapshotAt);
        snapshotRepository.save(snapshot);

        List<ImpressionSnapshotDto> result = impressionUseCase.getTimeSeries(
                snsAccountId,
                snapshotAt.minusSeconds(1),
                snapshotAt.plusSeconds(1));

        assertEquals(1, result.size());
        ImpressionSnapshotDto dto = result.get(0);
        assertEquals(snapshot.getId(), dto.id());
        assertEquals(snsAccountId, dto.snsAccountId());
        assertEquals("TWITTER", dto.platform());
        assertEquals(1000, dto.followersCount());
        assertEquals(200, dto.followingCount());
        assertEquals(50, dto.postCount());
        assertEquals(5000L, dto.impressionsCount());
        assertEquals(3.5, dto.engagementRate());
        assertEquals(snapshotAt, dto.snapshotAt());
    }

    @Test
    void getLatest_returnsLatestSnapshot() {
        Instant base = Instant.parse("2026-01-15T00:00:00Z");
        ImpressionSnapshot older = createSnapshot(snsAccountId, base);
        ImpressionSnapshot newer = createSnapshot(snsAccountId, base.plusSeconds(3600));
        snapshotRepository.save(older);
        snapshotRepository.save(newer);

        ImpressionSnapshotDto result = impressionUseCase.getLatest(snsAccountId);

        assertNotNull(result);
        assertEquals(newer.getId(), result.id());
    }

    @Test
    void getLatest_throwsSnapshotNotFoundForUnknownAccount() {
        UUID unknownAccountId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> impressionUseCase.getLatest(unknownAccountId));
        assertEquals("SNAPSHOT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getLatest_throwsSnapshotNotFoundWhenNoSnapshots() {
        DomainException exception = assertThrows(DomainException.class,
                () -> impressionUseCase.getLatest(snsAccountId));
        assertEquals("SNAPSHOT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void recordSnapshot_savesAndReturnsDto() {
        ImpressionSnapshot snapshot = createSnapshot(snsAccountId, Instant.now());

        ImpressionSnapshotDto result = impressionUseCase.recordSnapshot(snapshot);

        assertNotNull(result);
        assertEquals(snapshot.getId(), result.id());
        assertEquals(snsAccountId, result.snsAccountId());
    }

    @Test
    void recordSnapshot_persistsToRepository() {
        ImpressionSnapshot snapshot = createSnapshot(snsAccountId, Instant.now());

        impressionUseCase.recordSnapshot(snapshot);

        Optional<ImpressionSnapshot> stored = snapshotRepository.getLatestBySnsAccountId(snsAccountId);
        assertTrue(stored.isPresent());
        assertEquals(snapshot.getId(), stored.get().getId());
    }

    @Test
    void recordSnapshot_mapsDtoFieldsCorrectly() {
        Instant snapshotAt = Instant.parse("2026-06-01T09:00:00Z");
        ImpressionSnapshot snapshot = new ImpressionSnapshot(
                UUID.randomUUID(), snsAccountId, SnsPlatform.BLUESKY,
                500, 100, 25, 12000L, 4.2, snapshotAt);

        ImpressionSnapshotDto result = impressionUseCase.recordSnapshot(snapshot);

        assertEquals("BLUESKY", result.platform());
        assertEquals(500, result.followersCount());
        assertEquals(100, result.followingCount());
        assertEquals(25, result.postCount());
        assertEquals(12000L, result.impressionsCount());
        assertEquals(4.2, result.engagementRate());
        assertEquals(snapshotAt, result.snapshotAt());
    }

    private ImpressionSnapshot createSnapshot(UUID accountId, Instant snapshotAt) {
        return new ImpressionSnapshot(
                UUID.randomUUID(), accountId, SnsPlatform.TWITTER,
                100, 50, 10, 1000L, 2.5, snapshotAt);
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
                    .sorted(Comparator.comparing(ImpressionSnapshot::getSnapshotAt))
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
