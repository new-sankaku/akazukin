package com.akazukin.application.dto;

import java.util.Map;

public record AiCostMonitorDto(
    long totalCalls,
    Map<String, Long> callsByProvider
) {}
