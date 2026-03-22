package com.akazukin.application.dto;

import java.util.List;
import java.util.UUID;

public record RelationshipPlanDto(
    UUID friendId,
    String displayName,
    int relationshipScore,
    String analystNote,
    String recommendedPersona,
    List<PlanActionDto> actions
) {}
