package com.akazukin.domain.model;

public record TranslationRequest(
    String sourceText,
    String sourceLang,
    String targetLang
) {

    public TranslationRequest {
        if (sourceText == null || sourceText.isBlank()) {
            throw new IllegalArgumentException("sourceText must not be blank");
        }
    }
}
