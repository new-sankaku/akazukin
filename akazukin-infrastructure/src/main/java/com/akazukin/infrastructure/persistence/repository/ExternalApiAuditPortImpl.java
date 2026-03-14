package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AuditLogRepository;
import com.akazukin.domain.port.ExternalApiAuditPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ExternalApiAuditPortImpl implements ExternalApiAuditPort {

    @Inject
    AuditLogRepository auditLogRepository;

    @Override
    public void logExternalApiCall(
            SnsPlatform platform,
            String httpMethod,
            String endpoint,
            int responseStatus,
            long durationMs,
            String userId,
            String username) {

        UUID userUuid = userId != null ? UUID.fromString(userId) : null;
        String requestPath = platform.name() + " \u2192 " + endpoint;

        AuditLog auditLog = new AuditLog(
                null,
                userUuid,
                username,
                httpMethod,
                requestPath,
                null,
                null,
                responseStatus,
                durationMs,
                null,
                null,
                Instant.now(),
                "SNS_API"
        );

        auditLogRepository.save(auditLog);
    }
}
