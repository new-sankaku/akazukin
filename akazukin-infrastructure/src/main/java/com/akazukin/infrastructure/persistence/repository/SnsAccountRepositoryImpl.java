package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.infrastructure.persistence.entity.SnsAccountEntity;
import com.akazukin.infrastructure.persistence.mapper.SnsAccountMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SnsAccountRepositoryImpl implements SnsAccountRepository, PanacheRepository<SnsAccountEntity> {

    @Inject
    SnsAccountMapper snsAccountMapper;

    @Override
    public Optional<SnsAccount> findById(UUID id) {
        return find("id", id).firstResultOptional().map(snsAccountMapper::toDomain);
    }

    @Override
    public List<SnsAccount> findByUserId(UUID userId) {
        return find("userId", userId)
                .list()
                .stream()
                .map(snsAccountMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
        return find("userId = ?1 and platform = ?2", userId, platform.name())
                .firstResultOptional()
                .map(snsAccountMapper::toDomain);
    }

    @Override
    @Transactional
    public SnsAccount save(SnsAccount snsAccount) {
        SnsAccountEntity entity = snsAccountMapper.toEntity(snsAccount);
        if (entity.id != null && find("id", entity.id).firstResult() != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return snsAccountMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
