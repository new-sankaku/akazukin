package com.akazukin.application.dto;

import java.util.List;

public record LinkageScenarioDto(
    String name,
    List<StepDto> steps
) {

    public record StepDto(
        String platform,
        String action
    ) {
    }
}
