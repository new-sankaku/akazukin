package com.akazukin.domain.port;

import com.akazukin.domain.model.AiPersona;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiPersonaRepository {

    Optional<AiPersona> findById(UUID id);

    List<AiPersona> findByUserId(UUID userId);

    Optional<AiPersona> findDefaultByUserId(UUID userId);

    AiPersona save(AiPersona aiPersona);

    void deleteById(UUID id);
}
