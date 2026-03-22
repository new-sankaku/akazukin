package com.akazukin.web.api;

import com.akazukin.domain.model.AuditLog;
import com.akazukin.domain.port.AuditLogRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AuditLogResource {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int DEFAULT_RANGE_DAYS = 7;
    private static final int SECONDS_PER_DAY = 24 * 3600;

    @Inject
    AuditLogRepository auditLogRepository;

    @GET
    public Response list(@QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("50") int size,
                         @QueryParam("userId") UUID userId,
                         @QueryParam("path") String path,
                         @QueryParam("category") String category,
                         @QueryParam("from") String from,
                         @QueryParam("to") String to) {
        int effectiveSize = Math.min(size, PAGE_SIZE_MAX);
        int offset = page * effectiveSize;

        List<AuditLog> logs;
        if (category != null && !category.isBlank()) {
            logs = auditLogRepository.findByCategory(category, offset, effectiveSize);
        } else if (userId != null) {
            logs = auditLogRepository.findByUserId(userId, offset, effectiveSize);
        } else if (path != null && !path.isBlank()) {
            logs = auditLogRepository.findByRequestPath(path, offset, effectiveSize);
        } else if (from != null && to != null) {
            Instant fromInstant = LocalDate.parse(from).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant toInstant = LocalDate.parse(to).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            logs = auditLogRepository.findByCreatedAtBetween(fromInstant, toInstant, offset, effectiveSize);
        } else {
            Instant weekAgo = Instant.now().minusSeconds((long) DEFAULT_RANGE_DAYS * SECONDS_PER_DAY);
            logs = auditLogRepository.findByCreatedAtBetween(weekAgo, Instant.now(), offset, effectiveSize);
        }

        long totalCount = auditLogRepository.countAll();

        return Response.ok(Map.of(
                "data", logs,
                "totalCount", totalCount,
                "page", page,
                "size", effectiveSize
        )).build();
    }
}
