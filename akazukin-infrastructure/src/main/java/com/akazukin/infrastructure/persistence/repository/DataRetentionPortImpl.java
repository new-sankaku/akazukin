package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.port.DataRetentionPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class DataRetentionPortImpl implements DataRetentionPort {

    private final EntityManager entityManager;

    @Inject
    public DataRetentionPortImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int purgeAuditLogsBefore(Instant cutoff, int batchSize) {
        return entityManager
                .createNativeQuery(
                        "DELETE FROM audit_logs WHERE id IN ("
                                + "SELECT id FROM audit_logs WHERE created_at < :cutoff LIMIT :batchSize"
                                + ")")
                .setParameter("cutoff", cutoff)
                .setParameter("batchSize", batchSize)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int purgeReadNotificationsBefore(Instant cutoff, int batchSize) {
        return entityManager
                .createNativeQuery(
                        "DELETE FROM notifications WHERE id IN ("
                                + "SELECT id FROM notifications WHERE read = TRUE AND created_at < :cutoff LIMIT :batchSize"
                                + ")")
                .setParameter("cutoff", cutoff)
                .setParameter("batchSize", batchSize)
                .executeUpdate();
    }
}
