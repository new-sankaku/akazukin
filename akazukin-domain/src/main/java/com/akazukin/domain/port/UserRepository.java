package com.akazukin.domain.port;

import com.akazukin.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByIds(Collection<UUID> ids);

    User save(User user);

    void deleteById(UUID id);
}
