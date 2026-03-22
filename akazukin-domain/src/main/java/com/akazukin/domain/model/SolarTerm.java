package com.akazukin.domain.model;

import java.time.LocalDate;

public record SolarTerm(
    LocalDate date,
    String name,
    String nameEn,
    String hint,
    String hintEn
) {
}
