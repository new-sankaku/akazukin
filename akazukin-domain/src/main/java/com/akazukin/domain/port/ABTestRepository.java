package com.akazukin.domain.port;

import com.akazukin.domain.model.ABTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ABTestRepository {

    Optional<ABTest> findById(UUID id);

    List<ABTest> findByUserId(UUID userId);

    ABTest save(ABTest abTest);

    void deleteById(UUID id);

    List<ABTest> findByUserIdAndStatus(UUID userId, com.akazukin.domain.model.ABTestStatus status);

    List<ABTest> findCompletedByUserId(UUID userId);
}
