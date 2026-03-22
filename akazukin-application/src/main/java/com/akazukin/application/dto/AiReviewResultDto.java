package com.akazukin.application.dto;

import java.util.List;

public record AiReviewResultDto(
    int score,
    String verdict,
    List<AiReviewFindingDto> findings
) {}
