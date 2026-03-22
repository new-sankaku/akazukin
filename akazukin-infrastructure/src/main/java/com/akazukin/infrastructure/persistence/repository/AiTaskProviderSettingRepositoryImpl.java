package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AiTaskProviderSetting;
import com.akazukin.domain.model.AiTaskType;
import com.akazukin.domain.port.AiTaskProviderSettingRepository;
import com.akazukin.infrastructure.persistence.entity.AiTaskProviderSettingEntity;
import com.akazukin.infrastructure.persistence.mapper.AiTaskProviderSettingMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiTaskProviderSettingRepositoryImpl
        implements AiTaskProviderSettingRepository, PanacheRepository<AiTaskProviderSettingEntity> {

    @Override
    public List<AiTaskProviderSetting> findByUserId(UUID userId) {
        return find("userId", userId)
                .list()
                .stream()
                .map(AiTaskProviderSettingMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AiTaskProviderSetting> findByUserIdAndTaskType(UUID userId, AiTaskType taskType) {
        return find("userId = ?1 AND taskType = ?2", userId, taskType.name())
                .firstResultOptional()
                .map(AiTaskProviderSettingMapper::toDomain);
    }

    @Override
    @Transactional
    public AiTaskProviderSetting save(AiTaskProviderSetting setting) {
        AiTaskProviderSettingEntity entity = AiTaskProviderSettingMapper.toEntity(setting);
        if (entity.id == null) {
            entity.id = UUID.randomUUID();
            persist(entity);
        } else {
            entity = getEntityManager().merge(entity);
        }
        return AiTaskProviderSettingMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void saveAll(List<AiTaskProviderSetting> settings) {
        for (AiTaskProviderSetting setting : settings) {
            save(setting);
        }
    }
}
