package com.akazukin.domain.model;

import java.time.LocalDate;

public record JapaneseHoliday(
    LocalDate date,
    String name,
    String nameEn
) {
}
