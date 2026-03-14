package com.akazukin.domain.port;

import java.time.Instant;

public interface DataRetentionPort {

    int purgeAuditLogsBefore(Instant cutoff, int batchSize);

    int purgeReadNotificationsBefore(Instant cutoff, int batchSize);
}
