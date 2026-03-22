package com.akazukin.application.dto;

import java.util.List;

public record ComplianceCheckResultDto(
    List<ComplianceCheckItemDto> items,
    int passedCount,
    int totalCount,
    boolean allPassed
) {}
