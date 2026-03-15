package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.ImpressionSnapshot;
import com.akazukin.domain.port.ImpressionSnapshotRepository;
import com.akazukin.infrastructure.persistence.entity.ImpressionSnapshotEntity;
import com.akazukin.infrastructure.persistence.mapper.ImpressionSnapshotMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ImpressionSnapshotRepositoryImpl implements ImpressionSnapshotRepository, PanacheRepository<ImpressionSnapshotEntity> {

    private static final Logger LOG = Logger.getLogger(ImpressionSnapshotRepositoryImpl.class.getName());

    @Override
    public List<ImpressionSnapshot> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("snsAccountId = ?1 ORDER BY snapshotAt DESC", snsAccountId)
                    .page(offset / Math.max(limit, 1), Math.max(limit, 1))
                    .list()
                    .stream()
                    .map(ImpressionSnapshotMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.findBySnsAccountId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.findBySnsAccountId", perfMs});
            }
        }
    }

    @Override
    public List<ImpressionSnapshot> findBySnsAccountIdAndDateRange(UUID snsAccountId, Instant from, Instant to) {
        long perfStart = System.nanoTime();
        try {
            return find("snsAccountId = ?1 AND snapshotAt >= ?2 AND snapshotAt <= ?3 ORDER BY snapshotAt ASC",
                    snsAccountId, from, to)
                    .list()
                    .stream()
                    .map(ImpressionSnapshotMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.findBySnsAccountIdAndDateRange", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.findBySnsAccountIdAndDateRange", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public ImpressionSnapshot save(ImpressionSnapshot snapshot) {
        long perfStart = System.nanoTime();
        try {
            ImpressionSnapshotEntity entity = ImpressionSnapshotMapper.toEntity(snapshot);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return ImpressionSnapshotMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    public Optional<ImpressionSnapshot> getLatestBySnsAccountId(UUID snsAccountId) {
        long perfStart = System.nanoTime();
        try {
            return find("snsAccountId = ?1 ORDER BY snapshotAt DESC", snsAccountId)
                    .firstResultOptional()
                    .map(ImpressionSnapshotMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.getLatestBySnsAccountId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ImpressionSnapshotRepositoryImpl.getLatestBySnsAccountId", perfMs});
            }
        }
    }
}
