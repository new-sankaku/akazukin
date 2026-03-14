package com.akazukin.web.security;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.port.AuditLogRepository;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Provider
@Priority(Priorities.USER + 100)
public class AuditLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(AuditLogFilter.class);
    private static final String START_TIME_PROPERTY = "audit.startTimeNanos";
    private static final String REQUEST_BODY_PROPERTY = "audit.requestBody";
    private static final int MAX_BODY_LENGTH = 4096;

    private static final List<String> EXCLUDED_EXTENSIONS = List.of(
            ".js", ".css", ".png", ".ico", ".svg", ".woff2", ".map"
    );

    private static final List<String> EXCLUDED_PATH_PREFIXES = List.of(
            "/q/"
    );

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (shouldSkip(requestContext)) {
            return;
        }
        requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());

        if (BODY_METHODS.contains(requestContext.getMethod()) && requestContext.hasEntity()) {
            String body = captureRequestBody(requestContext);
            requestContext.setProperty(REQUEST_BODY_PROPERTY, body);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object startTimeObj = requestContext.getProperty(START_TIME_PROPERTY);
        if (startTimeObj == null) {
            return;
        }

        long startTime = (long) startTimeObj;
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        UUID userId = null;
        String username = null;
        try {
            if (jwt.getRawToken() != null && jwt.getSubject() != null) {
                userId = UUID.fromString(jwt.getSubject());
                username = jwt.getClaim("upn");
            }
        } catch (Exception ignored) {
            // JWT not available for public endpoints
        }

        String path = requestContext.getUriInfo().getPath();
        String queryString = requestContext.getUriInfo().getRequestUri().getQuery();
        String clientIp = extractClientIp(requestContext);
        String userAgent = requestContext.getHeaderString("User-Agent");
        if (userAgent != null && userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 512);
        }

        String requestBody = (String) requestContext.getProperty(REQUEST_BODY_PROPERTY);
        requestBody = sanitizeRequestBody(requestBody);

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String category = normalizedPath.startsWith("/api/") ? "INTERNAL_API" : "PAGE";

        AuditLog auditLog = new AuditLog(
                null,
                userId,
                username,
                requestContext.getMethod(),
                path,
                queryString,
                requestBody,
                responseContext.getStatus(),
                durationMs,
                clientIp,
                userAgent,
                Instant.now(),
                category
        );

        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            LOG.error("Failed to save audit log", e);
        }
    }

    private String captureRequestBody(ContainerRequestContext requestContext) throws IOException {
        InputStream originalStream = requestContext.getEntityStream();
        byte[] bodyBytes = originalStream.readAllBytes();

        // Reset the stream so downstream handlers can still read it
        requestContext.setEntityStream(new ByteArrayInputStream(bodyBytes));

        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY_LENGTH) {
            body = body.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
        }
        return body;
    }

    private String sanitizeRequestBody(String body) {
        if (body == null) {
            return null;
        }
        // Mask password fields in JSON
        return body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                   .replaceAll("\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"***\"")
                   .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                   .replaceAll("\"accessToken\"\\s*:\\s*\"[^\"]*\"", "\"accessToken\":\"***\"")
                   .replaceAll("\"refreshToken\"\\s*:\\s*\"[^\"]*\"", "\"refreshToken\":\"***\"")
                   .replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
    }

    private boolean shouldSkip(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (normalizedPath.startsWith(prefix)) {
                return true;
            }
        }

        for (String ext : EXCLUDED_EXTENSIONS) {
            if (normalizedPath.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    private String extractClientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return null;
    }
}
