package com.akazukin.application.dto;

public record RiskLevelFlowDto(
    String riskLevel,
    int requiredApprovers,
    boolean adminRequired,
    boolean legalReviewRequired
) {}
