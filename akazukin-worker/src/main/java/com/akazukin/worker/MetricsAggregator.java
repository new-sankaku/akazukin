package com.akazukin.worker;

import com.akazukin.domain.model.SnsPlatform;
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

    @Scheduled(every = "1h", identity = "metrics-aggregator")
    void aggregateMetrics() {
        LOG.log(Level.INFO, "Starting hourly metrics aggregation");

        try {
            long totalAccounts = snsAccountRepository.countAll();
            LOG.log(Level.INFO, "Total connected SNS accounts: {0}", totalAccounts);

            for (SnsPlatform platform : SnsPlatform.values()) {
                long accountCount = snsAccountRepository.countByPlatform(platform);
                if (accountCount > 0) {
                    LOG.log(Level.INFO, "Platform {0}: {1} connected accounts",
                            new Object[]{platform, accountCount});
                }
            }

            LOG.log(Level.INFO, "Metrics aggregation completed");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Metrics aggregation failed", e);
        }
    }
}
