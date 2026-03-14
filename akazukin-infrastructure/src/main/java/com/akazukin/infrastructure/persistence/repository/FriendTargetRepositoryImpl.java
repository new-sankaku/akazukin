package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.FriendTarget;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.FriendTargetRepository;
import com.akazukin.infrastructure.persistence.entity.FriendTargetEntity;
import com.akazukin.infrastructure.persistence.mapper.FriendTargetMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class FriendTargetRepositoryImpl implements FriendTargetRepository, PanacheRepository<FriendTargetEntity> {

    @Override
    public Optional<FriendTarget> findById(UUID id) {
        return find("id", id).firstResultOptional().map(FriendTargetMapper::toDomain);
    }

    @Override
    public List<FriendTarget> findByUserId(UUID userId) {
        return find("userId", userId)
                .list()
                .stream()
                .map(FriendTargetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendTarget> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
        return find("userId = ?1 and platform = ?2", userId, platform.name())
                .list()
                .stream()
                .map(FriendTargetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FriendTarget save(FriendTarget friendTarget) {
        FriendTargetEntity entity = FriendTargetMapper.toEntity(friendTarget);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return FriendTargetMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
