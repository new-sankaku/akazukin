package com.akazukin.domain.port;

import com.akazukin.domain.model.ImpressionSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImpressionSnapshotRepository {

    List<ImpressionSnapshot> findBySnsAccountId(UUID snsAccountId, int offset, int limit);

    List<ImpressionSnapshot> findBySnsAccountIdAndDateRange(UUID snsAccountId, Instant from, Instant to);

    ImpressionSnapshot save(ImpressionSnapshot snapshot);

    Optional<ImpressionSnapshot> getLatestBySnsAccountId(UUID snsAccountId);
}
