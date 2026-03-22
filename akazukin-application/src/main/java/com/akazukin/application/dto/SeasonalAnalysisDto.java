package com.akazukin.application.dto;

import java.util.List;

public record SeasonalAnalysisDto(
    List<SeasonalDataPointDto> dataPoints,
    List<String> platforms
) {}
