package com.akazukin.domain.model;

import java.time.LocalDate;
import java.util.List;

public record BridgePhase(
    int phaseNumber,
    String label,
    String labelEn,
    String title,
    String titleEn,
    List<BridgePhaseEntry> entries
) {

    public record BridgePhaseEntry(
        LocalDate date,
        String title,
        String titleEn
    ) {
    }
}
