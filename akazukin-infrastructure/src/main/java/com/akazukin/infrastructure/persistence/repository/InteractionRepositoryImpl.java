package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.Interaction;
import com.akazukin.domain.port.InteractionRepository;
import com.akazukin.infrastructure.persistence.entity.InteractionEntity;
import com.akazukin.infrastructure.persistence.mapper.InteractionMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class InteractionRepositoryImpl implements InteractionRepository, PanacheRepository<InteractionEntity> {

    @Override
    public List<Interaction> findByUserId(UUID userId, int offset, int limit) {
        return find("userId order by createdAt desc", userId)
                .page(offset / Math.max(limit, 1), Math.max(limit, 1))
                .list()
                .stream()
                .map(InteractionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Interaction> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
        return find("snsAccountId order by createdAt desc", snsAccountId)
                .page(offset / Math.max(limit, 1), Math.max(limit, 1))
                .list()
                .stream()
                .map(InteractionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Interaction save(Interaction interaction) {
        InteractionEntity entity = InteractionMapper.toEntity(interaction);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return InteractionMapper.toDomain(entity);
    }

    @Override
    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }
}
