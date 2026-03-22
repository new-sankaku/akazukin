package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.port.AuditLogRepository;
import com.akazukin.infrastructure.persistence.entity.AuditLogEntity;
import com.akazukin.infrastructure.persistence.mapper.AuditLogMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
            }
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

    @Override
    public List<AuditLog> findRecent(int limit) {
        long perfStart = System.nanoTime();
        try {
            return find("ORDER BY createdAt DESC")
                    .page(0, limit)
                    .list()
                    .stream()
                    .map(AuditLogMapper::toDomain)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findRecent", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.findRecent", perfMs});
            }
        }
    }

    @Override
    public Map<String, Long> countByResponseStatusRange() {
        long perfStart = System.nanoTime();
        try {
            Map<String, Long> result = new HashMap<>();
            long total = count();
            long successCount = count("responseStatus >= 200 AND responseStatus < 300");
            long warnCount = count("responseStatus >= 300 AND responseStatus < 500");
            long errorCount = count("responseStatus >= 500");
            result.put("total", total);
            result.put("success", successCount);
            result.put("warn", warnCount);
            result.put("error", errorCount);
            return result;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.countByResponseStatusRange", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AuditLogRepositoryImpl.countByResponseStatusRange", perfMs});
            }
        }
    }
}
