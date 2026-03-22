package com.akazukin.application.dto;

public record CorrelationCellDto(
    String platformA,
    String platformB,
    double correlation,
    String level
) {}
