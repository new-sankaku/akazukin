package com.akazukin.worker;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.SnsAdapter;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CircuitBreakerReporter {

    private static final Logger LOG = Logger.getLogger(CircuitBreakerReporter.class.getName());

    private final Map<SnsPlatform, SnsAdapter> adapterMap;

    @Inject
    public CircuitBreakerReporter(Instance<SnsAdapter> adapterInstances) {
        this.adapterMap = new EnumMap<>(SnsPlatform.class);
        for (SnsAdapter adapter : adapterInstances) {
            this.adapterMap.put(adapter.platform(), adapter);
        }
    }

    @Scheduled(every = "5m", identity = "circuit-breaker-reporter")
    void reportCircuitBreakerStatus() {
        if (adapterMap.isEmpty()) {
            LOG.log(Level.FINE, "No SNS adapters registered, skipping health report");
            return;
        }

        List<SnsPlatform> healthyPlatforms = new ArrayList<>();
        List<SnsPlatform> unhealthyPlatforms = new ArrayList<>();

        for (Map.Entry<SnsPlatform, SnsAdapter> entry : adapterMap.entrySet()) {
            try {
                entry.getValue().getMaxContentLength();
                healthyPlatforms.add(entry.getKey());
            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Health check failed for platform " + entry.getKey(), e);
                unhealthyPlatforms.add(entry.getKey());
            }
        }

        if (unhealthyPlatforms.isEmpty()) {
            LOG.log(Level.FINE, "All {0} SNS adapters healthy: {1}",
                    new Object[]{healthyPlatforms.size(), healthyPlatforms});
        } else {
            LOG.log(Level.WARNING,
                    "Unhealthy SNS adapters: {0} (healthy: {1})",
                    new Object[]{unhealthyPlatforms, healthyPlatforms});
        }
    }
}
