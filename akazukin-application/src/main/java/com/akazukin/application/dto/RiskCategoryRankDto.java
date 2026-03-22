package com.akazukin.application.dto;

public record RiskCategoryRankDto(
    String category,
    long count,
    double percentage
) {}
