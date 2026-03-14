package com.akazukin.domain.model;

import java.util.List;

public record ToneAnalysisResult(
    String toneLevel,
    double formalityScore,
    List<String> suggestions
) {
}
