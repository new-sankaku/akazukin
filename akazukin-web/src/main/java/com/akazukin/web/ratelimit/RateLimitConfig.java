package com.akazukin.web.ratelimit;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class RateLimitConfig {

    @ConfigProperty(name = "akazukin.rate-limit.general.capacity", defaultValue = "100")
    long generalCapacity;

    @ConfigProperty(name = "akazukin.rate-limit.general.period", defaultValue = "PT1M")
    Duration generalPeriod;

    @ConfigProperty(name = "akazukin.rate-limit.post.capacity", defaultValue = "30")
    long postCapacity;

    @ConfigProperty(name = "akazukin.rate-limit.post.period", defaultValue = "PT1M")
    Duration postPeriod;

    @ConfigProperty(name = "akazukin.rate-limit.ai.capacity", defaultValue = "10")
    long aiCapacity;

    @ConfigProperty(name = "akazukin.rate-limit.ai.period", defaultValue = "PT1M")
    Duration aiPeriod;

    public long getCapacity(EndpointCategory category) {
        return switch (category) {
            case POST -> postCapacity;
            case AI -> aiCapacity;
            case GENERAL -> generalCapacity;
        };
    }

    public Duration getPeriod(EndpointCategory category) {
        return switch (category) {
            case POST -> postPeriod;
            case AI -> aiPeriod;
            case GENERAL -> generalPeriod;
        };
    }
}
