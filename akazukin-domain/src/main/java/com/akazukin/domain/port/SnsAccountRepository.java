package com.akazukin.domain.port;

import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnsAccountRepository {

    Optional<SnsAccount> findById(UUID id);

    List<SnsAccount> findByUserId(UUID userId);

    Optional<SnsAccount> findByUserIdAndPlatform(UUID userId, SnsPlatform platform);

    SnsAccount save(SnsAccount snsAccount);

    void deleteById(UUID id);
}
