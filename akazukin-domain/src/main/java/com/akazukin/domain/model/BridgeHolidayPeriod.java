package com.akazukin.domain.model;

import java.time.LocalDate;

public record BridgeHolidayPeriod(
    String name,
    String nameEn,
    LocalDate startDate,
    LocalDate endDate
) {
}
