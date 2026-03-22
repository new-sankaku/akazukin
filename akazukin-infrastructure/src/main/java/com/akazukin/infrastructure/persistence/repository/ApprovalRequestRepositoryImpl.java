package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.domain.port.ApprovalRequestRepository;
import com.akazukin.infrastructure.persistence.entity.ApprovalRequestEntity;
import com.akazukin.infrastructure.persistence.mapper.ApprovalRequestMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class ApprovalRequestRepositoryImpl implements ApprovalRequestRepository, PanacheRepository<ApprovalRequestEntity> {

    private static final Logger LOG = Logger.getLogger(ApprovalRequestRepositoryImpl.class.getName());

    @Override
    public Optional<ApprovalRequest> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(ApprovalRequestMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public Optional<ApprovalRequest> findByPostId(UUID postId) {
        long perfStart = System.nanoTime();
        try {
            return find("postId", postId).firstResultOptional().map(ApprovalRequestMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findByPostId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findByPostId", perfMs});
            }
        }
    }

    @Override
    public List<ApprovalRequest> findPendingByApproverId(UUID approverId, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("approverId = ?1 AND decidedAt IS NULL", Sort.by("requestedAt").descending(), approverId)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(ApprovalRequestMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findPendingByApproverId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findPendingByApproverId", perfMs});
            }
        }
    }

    @Override
    public List<ApprovalRequest> findPendingByTeamId(UUID teamId, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("teamId = ?1 AND decidedAt IS NULL", Sort.by("requestedAt").descending(), teamId)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(ApprovalRequestMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findPendingByTeamId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findPendingByTeamId", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public ApprovalRequest save(ApprovalRequest approvalRequest) {
        long perfStart = System.nanoTime();
        try {
            ApprovalRequestEntity entity = ApprovalRequestMapper.toEntity(approvalRequest);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return ApprovalRequestMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    public long countPendingByApproverId(UUID approverId) {
        long perfStart = System.nanoTime();
        try {
            return count("approverId = ?1 AND decidedAt IS NULL", approverId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countPendingByApproverId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countPendingByApproverId", perfMs});
            }
        }
    }

    @Override
    public long countByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
        long perfStart = System.nanoTime();
        try {
            return count("teamId = ?1 AND status = ?2", teamId, status.name());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countByTeamIdAndStatus", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countByTeamIdAndStatus", perfMs});
            }
        }
    }

    @Override
    public long countByTeamIdAndStatusAndDecidedAfter(UUID teamId, ApprovalAction status, Instant after) {
        long perfStart = System.nanoTime();
        try {
            return count("teamId = ?1 AND status = ?2 AND decidedAt >= ?3", teamId, status.name(), after);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countByTeamIdAndStatusAndDecidedAfter", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.countByTeamIdAndStatusAndDecidedAfter", perfMs});
            }
        }
    }

    @Override
    public List<ApprovalRequest> findByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
        long perfStart = System.nanoTime();
        try {
            return find("teamId = ?1 AND status = ?2", teamId, status.name())
                    .list()
                    .stream()
                    .map(ApprovalRequestMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findByTeamIdAndStatus", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRequestRepositoryImpl.findByTeamIdAndStatus", perfMs});
            }
        }
    }
}
