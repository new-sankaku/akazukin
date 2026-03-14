package com.akazukin.application.usecase;

import com.akazukin.application.dto.ImpressionSnapshotDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.port.ImpressionSnapshotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ImpressionUseCase {

    private static final Logger LOG = Logger.getLogger(ImpressionUseCase.class.getName());

    private final ImpressionSnapshotRepository snapshotRepository;

    @Inject
    public ImpressionUseCase(ImpressionSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public List<ImpressionSnapshotDto> getTimeSeries(UUID snsAccountId, Instant from, Instant to) {
        if (from == null || to == null) {
            throw new DomainException("INVALID_INPUT", "Date range (from, to) is required");
        }

        return snapshotRepository.findBySnsAccountIdAndDateRange(snsAccountId, from, to).stream()
                .map(this::toDto)
                .toList();
    }

    public ImpressionSnapshotDto getLatest(UUID snsAccountId) {
        ImpressionSnapshot snapshot = snapshotRepository.getLatestBySnsAccountId(snsAccountId)
                .orElseThrow(() -> new DomainException("SNAPSHOT_NOT_FOUND",
                        "No impression snapshots found for account: " + snsAccountId));
        return toDto(snapshot);
    }

    public ImpressionSnapshotDto recordSnapshot(ImpressionSnapshot snapshot) {
        ImpressionSnapshot saved = snapshotRepository.save(snapshot);
        LOG.log(Level.INFO, "Impression snapshot recorded for account {0}", saved.getSnsAccountId());
        return toDto(saved);
    }

    private ImpressionSnapshotDto toDto(ImpressionSnapshot s) {
        return new ImpressionSnapshotDto(
                s.getId(),
                s.getSnsAccountId(),
                s.getPlatform().name(),
                s.getFollowersCount(),
                s.getFollowingCount(),
                s.getPostCount(),
                s.getImpressionsCount(),
                s.getEngagementRate(),
                s.getSnapshotAt()
        );
    }
}
