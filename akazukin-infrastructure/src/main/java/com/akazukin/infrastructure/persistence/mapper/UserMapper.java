package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.User;
import com.akazukin.infrastructure.persistence.entity.UserEntity;

public final class UserMapper {
    private UserMapper() {}

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.id,
                entity.username,
                entity.email,
                entity.passwordHash,
                Role.valueOf(entity.role),
                entity.createdAt,
                entity.updatedAt
        );
    }

    public static UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.id = domain.getId();
        entity.username = domain.getUsername();
        entity.email = domain.getEmail();
        entity.passwordHash = domain.getPasswordHash();
        entity.role = domain.getRole().name();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}
