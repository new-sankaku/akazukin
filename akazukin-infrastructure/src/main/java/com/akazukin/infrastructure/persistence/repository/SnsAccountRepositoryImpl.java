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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SnsAccountRepositoryImpl implements SnsAccountRepository, PanacheRepository<SnsAccountEntity> {

    private static final Logger LOG = Logger.getLogger(SnsAccountRepositoryImpl.class.getName());

    @Inject
    SnsAccountMapper snsAccountMapper;

    @Override
    public Optional<SnsAccount> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(snsAccountMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public List<SnsAccount> findAllByIds(Collection<UUID> ids) {
        long perfStart = System.nanoTime();
        try {
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            return find("id in ?1", List.copyOf(ids))
                    .list()
                    .stream()
                    .map(snsAccountMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findAllByIds", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findAllByIds", perfMs});
            }
        }
    }

    @Override
    public List<SnsAccount> findByUserId(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return find("userId", userId)
                    .list()
                    .stream()
                    .map(snsAccountMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findByUserId", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findByUserId", perfMs});
            }
        }
    }

    @Override
    public Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
        long perfStart = System.nanoTime();
        try {
            return find("userId = ?1 and platform = ?2", userId, platform.name())
                    .firstResultOptional()
                    .map(snsAccountMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findByUserIdAndPlatform", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.findByUserIdAndPlatform", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public SnsAccount save(SnsAccount snsAccount) {
        long perfStart = System.nanoTime();
        try {
            SnsAccountEntity entity = snsAccountMapper.toEntity(snsAccount);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return snsAccountMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            delete("id", id);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.deleteById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"SnsAccountRepositoryImpl.deleteById", perfMs});
            }
        }
    }

    @Override
    public long countAll() {
        return count();
    }

    @Override
    public long countByPlatform(SnsPlatform platform) {
        return count("platform", platform.name());
    }
}
