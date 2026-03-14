package com.akazukin.infrastructure.config;

import com.akazukin.infrastructure.persistence.entity.SnsAccountEntity;
import com.akazukin.infrastructure.persistence.entity.UserEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Dev profile only: seeds admin user and test SNS accounts on startup if no users exist.
 */
@ApplicationScoped
public class DevDataSeeder {

    private static final Logger LOG = Logger.getLogger(DevDataSeeder.class);

    private static final String[] TEST_PLATFORMS = {
        "TWITTER", "BLUESKY", "MASTODON", "THREADS", "INSTAGRAM",
        "REDDIT", "TELEGRAM", "VK", "PINTEREST"
    };

    @Inject
    EntityManager em;

    @ConfigProperty(name = "akazukin.dev.seed.enabled", defaultValue = "false")
    boolean seedEnabled;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (!seedEnabled) {
            return;
        }

        Long userCount = (Long) em.createQuery("SELECT COUNT(u) FROM UserEntity u").getSingleResult();
        if (userCount > 0) {
            LOG.infof("DevDataSeeder: %d users already exist, skipping seed.", userCount);
            return;
        }

        UserEntity admin = new UserEntity();
        admin.username = "admin";
        admin.email = "admin@akazukin.local";
        admin.passwordHash = hashPassword("admin123");
        admin.role = "ADMIN";
        em.persist(admin);
        em.flush();

        UserEntity testUser = new UserEntity();
        testUser.username = "testuser";
        testUser.email = "test@akazukin.local";
        testUser.passwordHash = hashPassword("test123");
        testUser.role = "USER";
        em.persist(testUser);
        em.flush();

        seedSnsAccounts(admin);

        LOG.info("DevDataSeeder: created initial users [admin / admin123] [testuser / test123]");
        LOG.infof("DevDataSeeder: created %d test SNS accounts for admin", TEST_PLATFORMS.length);
    }

    private void seedSnsAccounts(UserEntity owner) {
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
        for (String platform : TEST_PLATFORMS) {
            SnsAccountEntity account = new SnsAccountEntity();
            account.userId = owner.id;
            account.platform = platform;
            account.accountIdentifier = "dev_" + platform.toLowerCase();
            account.displayName = "Dev " + platform.charAt(0) + platform.substring(1).toLowerCase();
            account.accessToken = "dev-token-" + platform.toLowerCase();
            account.refreshToken = "dev-refresh-" + platform.toLowerCase();
            account.tokenExpiresAt = expiresAt;
            em.persist(account);
        }
    }

    private String hashPassword(String rawPassword) {
        try {
            // Fixed salt for dev seed data (reproducible)
            byte[] salt = "akazukin-dev-seed".getBytes(StandardCharsets.UTF_8);
            // Pad/trim to 16 bytes
            byte[] salt16 = new byte[16];
            System.arraycopy(salt, 0, salt16, 0, Math.min(salt.length, 16));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt16);
            byte[] hashed = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            String saltBase64 = Base64.getEncoder().encodeToString(salt16);
            String hashBase64 = Base64.getEncoder().encodeToString(hashed);
            return saltBase64 + ":" + hashBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
