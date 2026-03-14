package com.akazukin.web.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.sql.Connection;
import java.sql.SQLException;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    private static final String HEALTH_CHECK_NAME = "Database connection";
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final DataSource dataSource;

    @Inject
    public DatabaseHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(HEALTH_CHECK_NAME);
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(VALIDATION_TIMEOUT_SECONDS);
            if (valid) {
                builder.up()
                        .withData("database", connection.getMetaData().getDatabaseProductName())
                        .withData("url", connection.getMetaData().getURL());
            } else {
                builder.down()
                        .withData("reason", "Connection validation failed");
            }
        } catch (SQLException e) {
            builder.down()
                    .withData("reason", e.getMessage());
        }
        return builder.build();
    }
}
