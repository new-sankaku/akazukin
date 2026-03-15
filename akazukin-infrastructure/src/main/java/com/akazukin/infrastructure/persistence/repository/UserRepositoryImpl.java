package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.User;
import com.akazukin.domain.port.UserRepository;
import com.akazukin.infrastructure.persistence.entity.UserEntity;
import com.akazukin.infrastructure.persistence.mapper.UserMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepository<UserEntity> {

    private static final Logger LOG = Logger.getLogger(UserRepositoryImpl.class.getName());

    @Override
    public Optional<User> findById(UUID id) {
        long perfStart = System.nanoTime();
        try {
            return find("id", id).firstResultOptional().map(UserMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findById", perfMs});
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        long perfStart = System.nanoTime();
        try {
            return find("username", username).firstResultOptional().map(UserMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findByUsername", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findByUsername", perfMs});
            }
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        long perfStart = System.nanoTime();
        try {
            return find("email", email).firstResultOptional().map(UserMapper::toDomain);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findByEmail", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findByEmail", perfMs});
            }
        }
    }

    @Override
    public List<User> findAllByIds(Collection<UUID> ids) {
        long perfStart = System.nanoTime();
        try {
            if (ids.isEmpty()) {
                return List.of();
            }
            return find("id IN ?1", List.copyOf(ids))
                    .list()
                    .stream()
                    .map(UserMapper::toDomain)
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findAllByIds", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.findAllByIds", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public User save(User user) {
        long perfStart = System.nanoTime();
        try {
            UserEntity entity = UserMapper.toEntity(user);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                persist(entity);
            } else {
                entity = getEntityManager().merge(entity);
            }
            return UserMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.save", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.save", perfMs});
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
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.deleteById", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"UserRepositoryImpl.deleteById", perfMs});
            }
        }
    }
}
