package com.akazukin.application.dto;

public record NewsPostGeneratedDto(
    String generatedText,
    String newsTitle,
    String newsUrl
) {}
