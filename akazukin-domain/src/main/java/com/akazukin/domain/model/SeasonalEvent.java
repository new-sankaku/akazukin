package com.akazukin.domain.model;

import java.time.LocalDate;

public record SeasonalEvent(
    LocalDate date,
    String name,
    String nameEn,
    String hint,
    String hintEn
) {
}
