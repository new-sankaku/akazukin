package com.akazukin.worker;

import com.akazukin.domain.port.SnsAccountRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class MetricsAggregator {

    private static final Logger LOG = Logger.getLogger(MetricsAggregator.class.getName());

    private final SnsAccountRepository snsAccountRepository;

    @Inject
    public MetricsAggregator(SnsAccountRepository snsAccountRepository) {
        this.snsAccountRepository = snsAccountRepository;
    }

    // TODO: replace countAll() with countGroupByPlatform() to eliminate N+1 queries
    @Scheduled(every = "1h", identity = "metrics-aggregator")
    void aggregateMetrics() {
        try {
            long totalAccounts = snsAccountRepository.countAll();
            LOG.log(Level.INFO, "Total connected SNS accounts: {0}", totalAccounts);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Metrics aggregation failed", e);
        }
    }
}
