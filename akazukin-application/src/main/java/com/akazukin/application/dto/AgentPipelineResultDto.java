package com.akazukin.application.dto;

import java.util.List;

public record AgentPipelineResultDto(
    String analysis,
    String content,
    List<PlatformContentDto> platformContent,
    String riskAssessment,
    String complianceReport,
    String scheduleSuggestion
) {
}
