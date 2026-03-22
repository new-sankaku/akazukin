package com.akazukin.application.dto;

public record TrendWordDto(
    int rank,
    String word,
    long volume,
    String affinity
) {}
