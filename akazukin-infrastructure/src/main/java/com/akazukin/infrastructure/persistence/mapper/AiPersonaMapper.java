package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.infrastructure.persistence.entity.AiPersonaEntity;

public final class AiPersonaMapper {

    private AiPersonaMapper() {
    }

    public static AiPersona toDomain(AiPersonaEntity entity) {
        return new AiPersona(
                entity.id,
                entity.userId,
                entity.name,
                entity.systemPrompt,
                ContentTone.valueOf(entity.tone),
                entity.language,
                entity.avatarUrl,
                entity.isDefault,
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static AiPersonaEntity toEntity(AiPersona domain) {
        AiPersonaEntity entity = new AiPersonaEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.name = domain.getName();
        entity.systemPrompt = domain.getSystemPrompt();
        entity.tone = domain.getTone().name();
        entity.language = domain.getLanguage();
        entity.avatarUrl = domain.getAvatarUrl();
        entity.isDefault = domain.isDefault();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}
