package com.akazukin.domain.port;

import com.akazukin.domain.model.FriendTarget;
import com.akazukin.domain.model.SnsPlatform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendTargetRepository {

    Optional<FriendTarget> findById(UUID id);

    List<FriendTarget> findByUserId(UUID userId);

    List<FriendTarget> findByUserIdAndPlatform(UUID userId, SnsPlatform platform);

    FriendTarget save(FriendTarget friendTarget);

    void deleteById(UUID id);
}
