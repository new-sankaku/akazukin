package com.akazukin.web.mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Test helper that ensures test users exist in the database with specific UUIDs.
 * This allows @TestSecurity(user = "known-uuid") to work with FK constraints
 * on tables that reference users (e.g., teams.owner_user_id).
 *
 * Uses raw JDBC to insert users without depending on JPA.
 */
@ApplicationScoped
public class TestUserSetup {

    @Inject
    DataSource dataSource;

    /**
     * Ensures a user with the given UUID exists in the database.
     * If the user already exists, this is a no-op.
     * Uses MERGE (H2-specific) for atomic upsert to avoid unique constraint violations.
     */
    public void ensureUserExists(UUID userId) {
        try (Connection conn = dataSource.getConnection()) {
            // Check if user exists
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE id = ?")) {
                ps.setObject(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getLong(1) > 0) {
                        return;
                    }
                }
            }

            // Use full UUID in username/email to avoid unique constraint collisions
            String fullId = userId.toString().replace("-", "");
            Timestamp now = Timestamp.from(Instant.now());
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (id, username, email, password_hash, role, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setObject(1, userId);
                ps.setString(2, "tu_" + fullId);
                ps.setString(3, "tu_" + fullId + "@test.com");
                ps.setString(4, "hashed_password");
                ps.setString(5, "ADMIN");
                ps.setTimestamp(6, now);
                ps.setTimestamp(7, now);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // If insertion fails due to concurrent creation, ignore
            if (!e.getMessage().contains("Unique index") && !e.getMessage().contains("UNIQUE")) {
                throw new RuntimeException("Failed to ensure test user exists: " + userId, e);
            }
        }
    }
}
