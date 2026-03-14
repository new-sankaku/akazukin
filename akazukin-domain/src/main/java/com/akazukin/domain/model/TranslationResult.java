package com.akazukin.domain.model;

public record TranslationResult(
    String translatedText,
    String sourceLang,
    String targetLang,
    double confidence
) {
}
