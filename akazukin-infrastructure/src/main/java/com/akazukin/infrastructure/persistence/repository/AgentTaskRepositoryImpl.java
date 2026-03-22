package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AgentTaskRepository;
import com.akazukin.infrastructure.persistence.entity.AgentTaskEntity;
import com.akazukin.infrastructure.persistence.mapper.AgentTaskMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        if (entity.id == null) {
            entity.id = UUID.randomUUID();
            persist(entity);
        } else {
            entity = getEntityManager().merge(entity);
        }
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

    @Override
    public long countByUserId(UUID userId) {
        return count("userId", userId);
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, String status) {
        return count("userId = ?1 AND status = ?2", userId, status);
    }

    @Override
    public List<AgentTask> findByUserIdOrderByCreatedAt(UUID userId, int offset, int limit) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId)
                .page(offset / Math.max(limit, 1), Math.max(limit, 1))
                .list()
                .stream()
                .map(AgentTaskMapper::toDomain)
                .toList();
    }

    @Override
    public Map<AgentType, Long> countByUserIdGroupByAgentType(UUID userId) {
        List<Object[]> rows = getEntityManager()
                .createQuery("SELECT e.agentType, COUNT(e) FROM AgentTaskEntity e " +
                        "WHERE e.userId = ?1 GROUP BY e.agentType", Object[].class)
                .setParameter(1, userId)
                .getResultList();
        Map<AgentType, Long> result = new EnumMap<>(AgentType.class);
        for (Object[] row : rows) {
            try {
                AgentType type = AgentType.valueOf((String) row[0]);
                result.put(type, (Long) row[1]);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    @Override
    public Map<AgentType, Long> countByUserIdAndStatusGroupByAgentType(UUID userId, String status) {
        List<Object[]> rows = getEntityManager()
                .createQuery("SELECT e.agentType, COUNT(e) FROM AgentTaskEntity e " +
                        "WHERE e.userId = ?1 AND e.status = ?2 GROUP BY e.agentType", Object[].class)
                .setParameter(1, userId)
                .setParameter(2, status)
                .getResultList();
        Map<AgentType, Long> result = new EnumMap<>(AgentType.class);
        for (Object[] row : rows) {
            try {
                AgentType type = AgentType.valueOf((String) row[0]);
                result.put(type, (Long) row[1]);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    @Override
    public List<AgentTask> findByUserIdAndCreatedAtAfter(UUID userId, Instant after) {
        return find("userId = ?1 AND createdAt >= ?2 ORDER BY createdAt ASC", userId, after)
                .list()
                .stream()
                .map(AgentTaskMapper::toDomain)
                .toList();
    }
}
