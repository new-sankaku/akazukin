package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.infrastructure.persistence.entity.AiPersonaEntity;
import com.akazukin.infrastructure.persistence.mapper.AiPersonaMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiPersonaRepositoryImpl implements AiPersonaRepository, PanacheRepository<AiPersonaEntity> {

    @Override
    public Optional<AiPersona> findById(UUID id) {
        return find("id", id).firstResultOptional().map(AiPersonaMapper::toDomain);
    }

    @Override
    public List<AiPersona> findByUserId(UUID userId) {
        return find("userId", userId)
                .list()
                .stream()
                .map(AiPersonaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AiPersona> findDefaultByUserId(UUID userId) {
        return find("userId = ?1 AND isDefault = true", userId)
                .firstResultOptional()
                .map(AiPersonaMapper::toDomain);
    }

    @Override
    @Transactional
    public AiPersona save(AiPersona aiPersona) {
        AiPersonaEntity entity = AiPersonaMapper.toEntity(aiPersona);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return AiPersonaMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
