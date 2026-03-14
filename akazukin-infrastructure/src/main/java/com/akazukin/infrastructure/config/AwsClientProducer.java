package com.akazukin.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class AwsClientProducer {

    @ConfigProperty(name = "quarkus.sqs.endpoint-override")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "quarkus.sqs.aws.region", defaultValue = "ap-northeast-1")
    String region;

    @Produces
    @ApplicationScoped
    public SchedulerClient schedulerClient() {
        var builder = SchedulerClient.builder()
                .region(Region.of(region));
        endpointOverride.ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));
        return builder.build();
    }
}
