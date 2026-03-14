package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.port.AgentTaskRepository;
import com.akazukin.infrastructure.persistence.entity.AgentTaskEntity;
import com.akazukin.infrastructure.persistence.mapper.AgentTaskMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AgentTaskRepositoryImpl implements AgentTaskRepository, PanacheRepository<AgentTaskEntity> {

    @Override
    public Optional<AgentTask> findById(UUID id) {
        AgentTaskEntity entity = find("id", id).firstResult();
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(AgentTaskMapper.toDomain(entity));
    }

    @Override
    public List<AgentTask> findByUserId(UUID userId, int offset, int limit) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId)
                .page(offset / Math.max(limit, 1), Math.max(limit, 1))
                .list()
                .stream()
                .map(AgentTaskMapper::toDomain)
                .toList();
    }

    @Override
    public List<AgentTask> findByParentTaskId(UUID parentTaskId) {
        return find("parentTaskId = ?1 ORDER BY createdAt ASC", parentTaskId)
                .list()
                .stream()
                .map(AgentTaskMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public AgentTask save(AgentTask task) {
        AgentTaskEntity entity = AgentTaskMapper.toEntity(task);
        persist(entity);
        return AgentTaskMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, String status, String output) {
        AgentTaskEntity entity = find("id", id).firstResult();
        if (entity == null) {
            throw new IllegalArgumentException("Agent task not found: " + id);
        }
        entity.status = status;
        if (output != null) {
            entity.output = output;
        }
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            entity.completedAt = Instant.now();
        }
        persist(entity);
    }
}
