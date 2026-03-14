package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.port.AuditLogRepository;
import com.akazukin.infrastructure.persistence.entity.AuditLogEntity;
import com.akazukin.infrastructure.persistence.mapper.AuditLogMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuditLogRepositoryImpl implements AuditLogRepository, PanacheRepository<AuditLogEntity> {

    private static final Logger LOG = Logger.getLogger(AuditLogRepositoryImpl.class.getName());

    @Override
    @Transactional
    public void save(AuditLog auditLog) {
        long perfStart = System.nanoTime();
        try {
            AuditLogEntity entity = AuditLogMapper.toEntity(auditLog);
            persist(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    public List<AuditLog> findByUserId(UUID userId, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("userId = ?1 ORDER BY createdAt DESC", userId)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(AuditLogMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByUserId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByUserId", perfMs});
            }
        }
    }

    @Override
    public List<AuditLog> findByRequestPath(String pathPrefix, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("requestPath LIKE ?1 ORDER BY createdAt DESC", pathPrefix + "%")
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(AuditLogMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByRequestPath", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByRequestPath", perfMs});
            }
        }
    }

    @Override
    public List<AuditLog> findByCreatedAtBetween(Instant from, Instant to, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("createdAt >= ?1 AND createdAt <= ?2 ORDER BY createdAt DESC", from, to)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(AuditLogMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByCreatedAtBetween", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByCreatedAtBetween", perfMs});
            }
        }
    }

    @Override
    public List<AuditLog> findByCategory(String category, int offset, int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("category = ?1 ORDER BY createdAt DESC", category)
                    .page(offset / limit, limit)
                    .list()
                    .stream()
                    .map(AuditLogMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByCategory", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findByCategory", perfMs});
            }
        }
    }

    @Override
    public long countAll() {
        long perfStart = System.nanoTime();
        try {
            return count();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.countAll", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.countAll", perfMs});
            }
        }
    }
}
