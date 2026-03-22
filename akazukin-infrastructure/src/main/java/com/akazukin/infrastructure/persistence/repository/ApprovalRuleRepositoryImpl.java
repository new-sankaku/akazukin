package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.ApprovalRule;
import com.akazukin.domain.model.RiskLevelFlow;
import com.akazukin.domain.port.ApprovalRuleRepository;
import com.akazukin.infrastructure.persistence.entity.ApprovalRuleEntity;
import com.akazukin.infrastructure.persistence.entity.RiskLevelFlowEntity;
import com.akazukin.infrastructure.persistence.mapper.ApprovalRuleMapper;
import com.akazukin.infrastructure.persistence.mapper.RiskLevelFlowMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class ApprovalRuleRepositoryImpl implements ApprovalRuleRepository {

    private static final Logger LOG = Logger.getLogger(ApprovalRuleRepositoryImpl.class.getName());

    @Inject
    EntityManager entityManager;

    @Override
    public List<ApprovalRule> findByTeamId(UUID teamId) {
        long perfStart = System.nanoTime();
        try {
            List<ApprovalRuleEntity> entities = entityManager
                    .createQuery("SELECT e FROM ApprovalRuleEntity e WHERE e.teamId = :teamId", ApprovalRuleEntity.class)
                    .setParameter("teamId", teamId)
                    .getResultList();
            return entities.stream().map(ApprovalRuleMapper::toDomain).collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRuleRepositoryImpl.findByTeamId", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public ApprovalRule save(ApprovalRule rule) {
        long perfStart = System.nanoTime();
        try {
            ApprovalRuleEntity entity = ApprovalRuleMapper.toEntity(rule);
            if (entity.id == null) {
                entity.id = UUID.randomUUID();
                entityManager.persist(entity);
            } else {
                entity = entityManager.merge(entity);
            }
            return ApprovalRuleMapper.toDomain(entity);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRuleRepositoryImpl.save", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public void deleteByTeamId(UUID teamId) {
        entityManager.createQuery("DELETE FROM ApprovalRuleEntity e WHERE e.teamId = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
    }

    @Override
    public List<RiskLevelFlow> findRiskFlowsByTeamId(UUID teamId) {
        long perfStart = System.nanoTime();
        try {
            List<RiskLevelFlowEntity> entities = entityManager
                    .createQuery("SELECT e FROM RiskLevelFlowEntity e WHERE e.teamId = :teamId", RiskLevelFlowEntity.class)
                    .setParameter("teamId", teamId)
                    .getResultList();
            return entities.stream().map(RiskLevelFlowMapper::toDomain).collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalRuleRepositoryImpl.findRiskFlowsByTeamId", perfMs});
            }
        }
    }

    @Override
    @Transactional
    public RiskLevelFlow saveRiskFlow(RiskLevelFlow flow) {
        RiskLevelFlowEntity entity = RiskLevelFlowMapper.toEntity(flow);
        if (entity.id == null) {
            entity.id = UUID.randomUUID();
            entityManager.persist(entity);
        } else {
            entity = entityManager.merge(entity);
        }
        return RiskLevelFlowMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteRiskFlowsByTeamId(UUID teamId) {
        entityManager.createQuery("DELETE FROM RiskLevelFlowEntity e WHERE e.teamId = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
    }
}
