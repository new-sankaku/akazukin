package com.akazukin.domain.model;

import java.util.List;

public record LinkageScenario(
    String name,
    String nameEn,
    List<LinkageStep> steps
) {

    public record LinkageStep(
        String platform,
        String action,
        String actionEn
    ) {
    }
}
