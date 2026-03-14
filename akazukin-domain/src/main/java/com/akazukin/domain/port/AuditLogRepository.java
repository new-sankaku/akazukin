package com.akazukin.domain.port;

import com.akazukin.domain.model.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

    void save(AuditLog auditLog);

    List<AuditLog> findByUserId(UUID userId, int offset, int limit);

    List<AuditLog> findByRequestPath(String pathPrefix, int offset, int limit);

    List<AuditLog> findByCreatedAtBetween(Instant from, Instant to, int offset, int limit);

    List<AuditLog> findByCategory(String category, int offset, int limit);

    long countAll();
}
