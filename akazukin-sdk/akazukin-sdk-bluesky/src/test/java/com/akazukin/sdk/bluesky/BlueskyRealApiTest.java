package com.akazukin.sdk.bluesky;

import com.akazukin.sdk.bluesky.model.PostResponse;
import com.akazukin.sdk.bluesky.model.ProfileResponse;
import com.akazukin.sdk.bluesky.model.SessionResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BlueSky Real API integration test.
 *
 * Requires environment variables:
 *   BLUESKY_IDENTIFIER - BlueSky handle (e.g. "user.bsky.social")
 *   BLUESKY_APP_PASSWORD - App password
 *
 * Skipped automatically when env vars are not set.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "BLUESKY_IDENTIFIER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BLUESKY_APP_PASSWORD", matches = ".+")
class BlueskyRealApiTest {

    private final String identifier = System.getenv("BLUESKY_IDENTIFIER");
    private final String appPassword = System.getenv("BLUESKY_APP_PASSWORD");

    private BlueskyClient client;
    private SessionResponse session;
    private String postRkey;

    @BeforeAll
    void setUp() {
        client = BlueskyClient.builder()
                .config(BlueskyConfig.defaultConfig())
                .build();
    }

    @Test
    @Order(1)
    void createSession_shouldAuthenticate() {
        session = client.createSession(identifier, appPassword);

        assertNotNull(session);
        assertNotNull(session.did());
        assertNotNull(session.accessJwt());
        assertNotNull(session.refreshJwt());
        assertNotNull(session.handle());

        System.out.println("=== Session Created ===");
        System.out.println("DID: " + session.did());
        System.out.println("Handle: " + session.handle());
    }

    @Test
    @Order(2)
    void getProfile_shouldReturnProfile() {
        assertNotNull(session, "Session must be created first");

        ProfileResponse profile = client.getProfile(session.accessJwt(), session.did());

        assertNotNull(profile);
        assertNotNull(profile.handle());
        assertNotNull(profile.did());

        System.out.println("=== Profile ===");
        System.out.println("Handle: " + profile.handle());
        System.out.println("Display Name: " + profile.displayName());
        System.out.println("Avatar: " + profile.avatar());
        System.out.println("Followers: " + profile.followersCount());
        System.out.println("Following: " + profile.followsCount());
        System.out.println("Posts: " + profile.postsCount());
    }

    @Test
    @Order(3)
    void createPost_shouldPublish() {
        assertNotNull(session, "Session must be created first");

        String text = "[Akazukin Test] BlueskyClient integration test - " + System.currentTimeMillis();
        PostResponse post = client.createPost(session.accessJwt(), session.did(), text);

        assertNotNull(post);
        assertNotNull(post.uri());
        assertNotNull(post.cid());
        assertTrue(post.uri().contains("app.bsky.feed.post"));

        postRkey = post.uri().substring(post.uri().lastIndexOf('/') + 1);

        System.out.println("=== Post Created ===");
        System.out.println("URI: " + post.uri());
        System.out.println("CID: " + post.cid());
        System.out.println("Rkey: " + postRkey);
    }

    @Test
    @Order(4)
    void deletePost_shouldRemove() {
        assertNotNull(session, "Session must be created first");
        assertNotNull(postRkey, "Post must be created first");

        client.deletePost(session.accessJwt(), session.did(), postRkey);

        System.out.println("=== Post Deleted ===");
        System.out.println("Rkey: " + postRkey);
    }

    @Test
    @Order(5)
    void refreshSession_shouldGetNewTokens() {
        assertNotNull(session, "Session must be created first");

        SessionResponse refreshed = client.refreshSession(session.refreshJwt());

        assertNotNull(refreshed);
        assertNotNull(refreshed.accessJwt());
        assertNotNull(refreshed.refreshJwt());
        assertFalse(refreshed.accessJwt().isEmpty());

        System.out.println("=== Session Refreshed ===");
        System.out.println("New access token obtained successfully");
    }
}
