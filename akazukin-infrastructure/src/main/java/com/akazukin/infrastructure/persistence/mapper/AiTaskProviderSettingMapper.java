package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.AiModelProvider;
import com.akazukin.domain.model.AiTaskProviderSetting;
import com.akazukin.domain.model.AiTaskType;
import com.akazukin.infrastructure.persistence.entity.AiTaskProviderSettingEntity;

public final class AiTaskProviderSettingMapper {

    private AiTaskProviderSettingMapper() {
    }

    public static AiTaskProviderSetting toDomain(AiTaskProviderSettingEntity entity) {
        return new AiTaskProviderSetting(
                entity.id,
                entity.userId,
                AiTaskType.valueOf(entity.taskType),
                AiModelProvider.valueOf(entity.provider)
        );
    }

    public static AiTaskProviderSettingEntity toEntity(AiTaskProviderSetting domain) {
        AiTaskProviderSettingEntity entity = new AiTaskProviderSettingEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.taskType = domain.getTaskType().name();
        entity.provider = domain.getProvider().name();
        return entity;
    }
}
