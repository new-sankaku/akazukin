package com.akazukin.application.dto;

import java.util.List;

public record PodMonitorDto(
    long totalEvents,
    long successCount,
    long warnCount,
    long errorCount,
    List<AuditLogEntryDto> entries
) {}
