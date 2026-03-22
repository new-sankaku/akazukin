package com.akazukin.application.dto;

import java.util.UUID;

public record TeamApprovalStatusDto(
    UUID teamId,
    String teamName,
    long pendingCount,
    long approvedCount,
    long rejectedCount,
    double failRate
) {}
