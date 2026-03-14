package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.infrastructure.persistence.entity.AuditLogEntity;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLog toDomain(AuditLogEntity entity) {
        return new AuditLog(
                entity.id,
                entity.userId,
                entity.username,
                entity.httpMethod,
                entity.requestPath,
                entity.queryString,
                entity.requestBody,
                entity.responseStatus,
                entity.durationMs,
                entity.clientIp,
                entity.userAgent,
                entity.createdAt,
                entity.category
        );
    }

    public static AuditLogEntity toEntity(AuditLog domain) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.username = domain.getUsername();
        entity.httpMethod = domain.getHttpMethod();
        entity.requestPath = domain.getRequestPath();
        entity.queryString = domain.getQueryString();
        entity.requestBody = domain.getRequestBody();
        entity.responseStatus = domain.getResponseStatus();
        entity.durationMs = domain.getDurationMs();
        entity.clientIp = domain.getClientIp();
        entity.userAgent = domain.getUserAgent();
        entity.createdAt = domain.getCreatedAt();
        entity.category = domain.getCategory() != null ? domain.getCategory() : "PAGE";
        return entity;
    }
}
