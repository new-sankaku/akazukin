package com.akazukin.application.dto;

import java.time.Instant;

public record AuditLogEntryDto(
    String timestamp,
    String level,
    String platform,
    String message
) {}
