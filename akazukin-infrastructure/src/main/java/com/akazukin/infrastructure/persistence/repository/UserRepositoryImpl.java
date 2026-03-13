package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.User;
import com.akazukin.domain.port.UserRepository;
import com.akazukin.infrastructure.persistence.entity.UserEntity;
import com.akazukin.infrastructure.persistence.mapper.UserMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepositoryImpl implements UserRepository, PanacheRepository<UserEntity> {

    @Override
    public Optional<User> findById(UUID id) {
        return find("id", id).firstResultOptional().map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional().map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional().map(UserMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        if (entity.id != null) {
            entity = getEntityManager().merge(entity);
        } else {
            persist(entity);
        }
        return UserMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
