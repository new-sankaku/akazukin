package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.port.ABTestRepository;
import com.akazukin.infrastructure.persistence.entity.ABTestEntity;
import com.akazukin.infrastructure.persistence.mapper.ABTestMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ABTestRepositoryImpl implements ABTestRepository, PanacheRepository<ABTestEntity> {

    @Override
    public Optional<ABTest> findById(UUID id) {
        return find("id", id)
                .firstResultOptional()
                .map(ABTestMapper::toDomain);
    }

    @Override
    public List<ABTest> findByUserId(UUID userId) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId)
                .list()
                .stream()
                .map(ABTestMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public ABTest save(ABTest abTest) {
        ABTestEntity entity = ABTestMapper.toEntity(abTest);
        ABTestEntity existing = find("id", entity.id).firstResult();
        if (existing != null) {
            existing.name = entity.name;
            existing.variantA = entity.variantA;
            existing.variantB = entity.variantB;
            existing.status = entity.status;
            existing.startedAt = entity.startedAt;
            existing.completedAt = entity.completedAt;
            existing.winnerVariant = entity.winnerVariant;
            persist(existing);
            return ABTestMapper.toDomain(existing);
        }
        persist(entity);
        return ABTestMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
