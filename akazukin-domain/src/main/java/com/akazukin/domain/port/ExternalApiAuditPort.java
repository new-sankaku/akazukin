package com.akazukin.domain.port;

import com.akazukin.domain.model.SnsPlatform;

public interface ExternalApiAuditPort {
    void logExternalApiCall(
        SnsPlatform platform,
        String httpMethod,
        String endpoint,
        int responseStatus,
        long durationMs,
        String userId,
        String username
    );
}
