package com.akazukin.domain.port;

import com.akazukin.domain.model.AiTaskProviderSetting;
import com.akazukin.domain.model.AiTaskType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiTaskProviderSettingRepository {

    List<AiTaskProviderSetting> findByUserId(UUID userId);

    Optional<AiTaskProviderSetting> findByUserIdAndTaskType(UUID userId, AiTaskType taskType);

    AiTaskProviderSetting save(AiTaskProviderSetting setting);

    void saveAll(List<AiTaskProviderSetting> settings);
}
