package com.akazukin.domain.port;

import com.akazukin.domain.model.Interaction;

import java.util.List;
import java.util.UUID;

public interface InteractionRepository {

    List<Interaction> findByUserId(UUID userId, int offset, int limit);

    List<Interaction> findBySnsAccountId(UUID snsAccountId, int offset, int limit);

    Interaction save(Interaction interaction);

    long countByUserId(UUID userId);
}
