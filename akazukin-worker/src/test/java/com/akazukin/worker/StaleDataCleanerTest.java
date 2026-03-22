package com.akazukin.worker;

import com.akazukin.domain.port.DataRetentionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleDataCleanerTest {

    @Mock
    private DataRetentionPort dataRetentionPort;

    private StaleDataCleaner staleDataCleaner;

    @BeforeEach
    void setUp() {
        staleDataCleaner = new StaleDataCleaner(dataRetentionPort, 90, 30);
    }

    @Test
    void cleanStaleData_purgesAuditLogsAndNotifications() {
        when(dataRetentionPort.purgeAuditLogsBefore(any(Instant.class), eq(1000)))
                .thenReturn(0);
        when(dataRetentionPort.purgeReadNotificationsBefore(any(Instant.class), eq(1000)))
                .thenReturn(0);

        staleDataCleaner.cleanStaleData();

        verify(dataRetentionPort).purgeAuditLogsBefore(any(Instant.class), eq(1000));
        verify(dataRetentionPort).purgeReadNotificationsBefore(any(Instant.class), eq(1000));
    }

    @Test
    void cleanStaleData_repeatsUntilBatchNotFull() {
        when(dataRetentionPort.purgeAuditLogsBefore(any(Instant.class), eq(1000)))
                .thenReturn(1000)
                .thenReturn(1000)
                .thenReturn(500);
        when(dataRetentionPort.purgeReadNotificationsBefore(any(Instant.class), eq(1000)))
                .thenReturn(0);

        staleDataCleaner.cleanStaleData();

        verify(dataRetentionPort, times(3))
                .purgeAuditLogsBefore(any(Instant.class), eq(1000));
    }

    @Test
    void cleanStaleData_stopsAtMaxIterations() {
        when(dataRetentionPort.purgeAuditLogsBefore(any(Instant.class), eq(1000)))
                .thenReturn(1000);
        when(dataRetentionPort.purgeReadNotificationsBefore(any(Instant.class), eq(1000)))
                .thenReturn(0);

        staleDataCleaner.cleanStaleData();

        verify(dataRetentionPort, times(1000))
                .purgeAuditLogsBefore(any(Instant.class), eq(1000));
    }

    @Test
    void cleanStaleData_continuesNotificationPurgeWhenAuditLogPurgeFails() {
        when(dataRetentionPort.purgeAuditLogsBefore(any(Instant.class), eq(1000)))
                .thenThrow(new RuntimeException("db error"));
        when(dataRetentionPort.purgeReadNotificationsBefore(any(Instant.class), eq(1000)))
                .thenReturn(200);

        staleDataCleaner.cleanStaleData();

        verify(dataRetentionPort).purgeAuditLogsBefore(any(Instant.class), eq(1000));
        verify(dataRetentionPort).purgeReadNotificationsBefore(any(Instant.class), eq(1000));
    }

    @Test
    void cleanStaleData_handlesNotificationPurgeInBatches() {
        when(dataRetentionPort.purgeAuditLogsBefore(any(Instant.class), eq(1000)))
                .thenReturn(0);
        when(dataRetentionPort.purgeReadNotificationsBefore(any(Instant.class), eq(1000)))
                .thenReturn(1000)
                .thenReturn(300);

        staleDataCleaner.cleanStaleData();

        verify(dataRetentionPort, times(2))
                .purgeReadNotificationsBefore(any(Instant.class), eq(1000));
    }
}
