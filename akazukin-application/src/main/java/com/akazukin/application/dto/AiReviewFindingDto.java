package com.akazukin.application.dto;

import java.util.List;

public record AiReviewFindingDto(
    String severity,
    String title,
    String description,
    List<String> relatedLaws,
    String pastCase
) {}
