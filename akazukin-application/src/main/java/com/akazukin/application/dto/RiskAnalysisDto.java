package com.akazukin.application.dto;

import java.util.List;

public record RiskAnalysisDto(
    List<RiskTrendPointDto> trendPoints,
    List<RiskCategoryRankDto> categoryRanking
) {}
