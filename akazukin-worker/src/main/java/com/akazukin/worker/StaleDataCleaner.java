package com.akazukin.worker;

import com.akazukin.domain.port.DataRetentionPort;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class StaleDataCleaner {

    private static final Logger LOG = Logger.getLogger(StaleDataCleaner.class.getName());
    private static final int BATCH_SIZE = 1000;

    private final DataRetentionPort dataRetentionPort;
    private final Duration auditLogRetention;
    private final Duration notificationRetention;

    @Inject
    public StaleDataCleaner(DataRetentionPort dataRetentionPort,
                            @ConfigProperty(name = "akazukin.retention.audit-log-days",
                                    defaultValue = "90") int auditLogDays,
                            @ConfigProperty(name = "akazukin.retention.notification-days",
                                    defaultValue = "30") int notificationDays) {
        this.dataRetentionPort = dataRetentionPort;
        this.auditLogRetention = Duration.ofDays(auditLogDays);
        this.notificationRetention = Duration.ofDays(notificationDays);
    }

    @Scheduled(cron = "0 0 3 * * ?", identity = "stale-data-cleaner")
    void cleanStaleData() {
        LOG.log(Level.INFO, "Starting stale data cleanup");

        try {
            int totalAuditLogsDeleted = purgeInBatches(this::purgeAuditLogs);
            LOG.log(Level.INFO, "Purged {0} audit logs older than {1} days",
                    new Object[]{totalAuditLogsDeleted, auditLogRetention.toDays()});
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to purge audit logs", e);
        }

        try {
            int totalNotificationsDeleted = purgeInBatches(this::purgeNotifications);
            LOG.log(Level.INFO, "Purged {0} read notifications older than {1} days",
                    new Object[]{totalNotificationsDeleted, notificationRetention.toDays()});
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to purge notifications", e);
        }

        LOG.log(Level.INFO, "Stale data cleanup completed");
    }

    private int purgeAuditLogs() {
        Instant cutoff = Instant.now().minus(auditLogRetention);
        return dataRetentionPort.purgeAuditLogsBefore(cutoff, BATCH_SIZE);
    }

    private int purgeNotifications() {
        Instant cutoff = Instant.now().minus(notificationRetention);
        return dataRetentionPort.purgeReadNotificationsBefore(cutoff, BATCH_SIZE);
    }

    private int purgeInBatches(PurgeOperation operation) {
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = operation.execute();
            totalDeleted += deleted;
        } while (deleted == BATCH_SIZE);
        return totalDeleted;
    }

    @FunctionalInterface
    private interface PurgeOperation {
        int execute();
    }
}
