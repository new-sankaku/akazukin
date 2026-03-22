package com.akazukin.application.dto;

import java.util.List;

public record ABTestLoserAnalysisDto(
    List<String> winFactors,
    String suggestedNextTestName,
    String suggestedNextTestText,
    String aiSummary
) {}
