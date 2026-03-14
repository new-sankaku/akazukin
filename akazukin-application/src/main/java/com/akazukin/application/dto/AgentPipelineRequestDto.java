package com.akazukin.application.dto;

import java.util.List;

public record AgentPipelineRequestDto(
    String topic,
    List<String> targetPlatforms
) {
}
