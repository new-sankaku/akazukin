package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.model.ABTestStatus;
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
        if (entity.id == null) {
            entity.id = UUID.randomUUID();
            persist(entity);
        } else {
            entity = getEntityManager().merge(entity);
        }
        return ABTestMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }

    @Override
    public List<ABTest> findByUserIdAndStatus(UUID userId, ABTestStatus status) {
        return find("userId = ?1 AND status = ?2 ORDER BY createdAt DESC", userId, status.name())
                .list()
                .stream()
                .map(ABTestMapper::toDomain)
                .toList();
    }

    @Override
    public List<ABTest> findCompletedByUserId(UUID userId) {
        return findByUserIdAndStatus(userId, ABTestStatus.COMPLETED);
    }
}
