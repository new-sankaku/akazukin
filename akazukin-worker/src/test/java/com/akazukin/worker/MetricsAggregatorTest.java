package com.akazukin.worker;

import com.akazukin.domain.port.SnsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsAggregatorTest {

    @Mock
    private SnsAccountRepository snsAccountRepository;

    private MetricsAggregator metricsAggregator;

    @BeforeEach
    void setUp() {
        metricsAggregator = new MetricsAggregator(snsAccountRepository);
    }

    @Test
    void aggregateMetrics_countsAllAccounts() {
        when(snsAccountRepository.countAll()).thenReturn(42L);

        metricsAggregator.aggregateMetrics();

        verify(snsAccountRepository).countAll();
    }

    @Test
    void aggregateMetrics_handlesZeroAccounts() {
        when(snsAccountRepository.countAll()).thenReturn(0L);

        metricsAggregator.aggregateMetrics();

        verify(snsAccountRepository).countAll();
    }

    @Test
    void aggregateMetrics_completesNormallyWhenRepositoryThrows() {
        when(snsAccountRepository.countAll()).thenThrow(new RuntimeException("db error"));

        metricsAggregator.aggregateMetrics();

        verify(snsAccountRepository).countAll();
    }
}
