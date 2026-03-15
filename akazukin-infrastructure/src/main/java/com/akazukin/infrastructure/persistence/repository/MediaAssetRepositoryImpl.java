package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.MediaAsset;
import com.akazukin.domain.port.MediaAssetRepository;
import com.akazukin.infrastructure.persistence.entity.MediaAssetEntity;
import com.akazukin.infrastructure.persistence.mapper.MediaAssetMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MediaAssetRepositoryImpl implements MediaAssetRepository, PanacheRepository<MediaAssetEntity> {

    @Override
    public Optional<MediaAsset> findById(UUID id) {
        return find("id", id).firstResultOptional().map(MediaAssetMapper::toDomain);
    }

    @Override
    public List<MediaAsset> findByUserId(UUID userId, int offset, int limit) {
        return find("userId", Sort.by("createdAt").descending(), userId)
                .page(offset / limit, limit)
                .list()
                .stream()
                .map(MediaAssetMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MediaAsset save(MediaAsset mediaAsset) {
        MediaAssetEntity entity = MediaAssetMapper.toEntity(mediaAsset);
        if (entity.id == null) {
            entity.id = UUID.randomUUID();
            persist(entity);
        } else {
            entity = getEntityManager().merge(entity);
        }
        return MediaAssetMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }

    @Override
    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }
}
