package com.akazukin.application.dto;

import java.time.LocalDate;
import java.util.List;

public record BridgeHolidayPlanDto(
    String periodName,
    LocalDate startDate,
    LocalDate endDate,
    List<PhaseDto> phases
) {

    public record PhaseDto(
        int phaseNumber,
        String label,
        String title,
        List<PhaseEntryDto> entries
    ) {
    }

    public record PhaseEntryDto(
        LocalDate date,
        String title
    ) {
    }
}
