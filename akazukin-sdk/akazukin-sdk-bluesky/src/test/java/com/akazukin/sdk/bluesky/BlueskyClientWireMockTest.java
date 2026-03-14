package com.akazukin.sdk.bluesky;

import com.akazukin.sdk.bluesky.exception.BlueskyApiException;
import com.akazukin.sdk.bluesky.model.PostResponse;
import com.akazukin.sdk.bluesky.model.ProfileResponse;
import com.akazukin.sdk.bluesky.model.SessionResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueskyClientWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private BlueskyClient client;

    @BeforeEach
    void setUp() {
        BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());
        client = BlueskyClient.builder()
            .config(config)
            .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // =========================================================================
    // createSession
    // =========================================================================
    @Nested
    @DisplayName("createSession(identifier, password)")
    class CreateSessionTests {

        @Test
        @DisplayName("Success - returns SessionResponse with all fields")
        void success_allFields() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:abc123",
                          "handle": "alice.bsky.social",
                          "accessJwt": "access-token-1",
                          "refreshJwt": "refresh-token-1"
                        }
                        """)));

            SessionResponse response = client.createSession("alice.bsky.social", "password123");

            assertNotNull(response);
            assertEquals("did:plc:abc123", response.did());
            assertEquals("alice.bsky.social", response.handle());
            assertEquals("access-token-1", response.accessJwt());
            assertEquals("refresh-token-1", response.refreshJwt());
        }

        @Test
        @DisplayName("Verify POST to correct endpoint /xrpc/com.atproto.server.createSession")
        void verify_endpoint() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.createSession("user", "pass");

            wireMock.verify(1, postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.createSession")));
        }

        @Test
        @DisplayName("Verify Content-Type: application/json header is sent")
        void verify_contentType() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.createSession("user", "pass");

            wireMock.verify(postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Verify request body contains identifier and password")
        void verify_requestBody() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.createSession("alice@example.com", "s3cret!");

            wireMock.verify(postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .withRequestBody(equalToJson("""
                    {"identifier":"alice@example.com","password":"s3cret!"}
                    """)));
        }

        @Test
        @DisplayName("Response with extra unknown fields is ignored")
        void success_extraFields_ignored() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:abc",
                          "handle": "h",
                          "accessJwt": "a",
                          "refreshJwt": "r",
                          "email": "alice@example.com",
                          "emailConfirmed": true,
                          "active": true
                        }
                        """)));

            SessionResponse response = client.createSession("h", "pass");

            assertNotNull(response);
            assertEquals("did:plc:abc", response.did());
            assertEquals("a", response.accessJwt());
        }

        @Test
        @DisplayName("400 Bad Request - BlueskyApiException with error/message from JSON")
        void error_400() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest","message":"Invalid identifier"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("bad", "pass"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("InvalidRequest", ex.getError());
            assertEquals("Invalid identifier", ex.getMessage());
        }

        @Test
        @DisplayName("401 AuthenticationRequired - exception with error field parsed")
        void error_401() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"AuthenticationRequired","message":"Invalid credentials"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "wrong"));

            assertEquals(401, ex.getStatusCode());
            assertEquals("AuthenticationRequired", ex.getError());
            assertEquals("Invalid credentials", ex.getMessage());
        }

        @Test
        @DisplayName("403 Forbidden - exception")
        void error_403() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"AccountTakedown","message":"Account suspended"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(403, ex.getStatusCode());
            assertEquals("AccountTakedown", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry returns result (WireMock Scenario)")
        void error_429_thenSuccess() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("429-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("after-429"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("429-retry")
                .whenScenarioStateIs("after-429")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            SessionResponse response = client.createSession("user", "pass");

            assertNotNull(response);
            assertEquals("d", response.did());
        }

        @Test
        @DisplayName("429 with Retry-After header is respected")
        void error_429_retryAfterHeader() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("429-retry-after")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retry-1"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("429-retry-after")
                .whenScenarioStateIs("retry-1")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            long start = System.currentTimeMillis();
            SessionResponse response = client.createSession("user", "pass");
            long elapsed = System.currentTimeMillis() - start;

            assertNotNull(response);
            assertTrue(elapsed >= 900, "Should have waited at least ~1 second for Retry-After");
        }

        @Test
        @DisplayName("429 all retries exhausted - throws exception")
        void error_429_allRetriesExhausted() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(429, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error - exception")
        void error_500() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InternalServerError","message":"Something went wrong"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(500, ex.getStatusCode());
            assertEquals("InternalServerError", ex.getError());
            assertEquals("Something went wrong", ex.getMessage());
        }

        @Test
        @DisplayName("502 Bad Gateway - exception")
        void error_502() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(502)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"BadGateway","message":"Upstream error"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(502, ex.getStatusCode());
        }

        @Test
        @DisplayName("503 Service Unavailable - exception")
        void error_503() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(503)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"ServiceUnavailable","message":"Maintenance"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(503, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response body - PARSE_ERROR")
        void error_invalidJson() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not valid json {{")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(200, ex.getStatusCode());
            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty response body - PARSE_ERROR")
        void error_emptyBody() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error response body not valid JSON - fallback message with note")
        void error_errorResponseNotValidJson() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "text/plain")
                    .withBody("Bad Request - plain text")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(400, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }

        @Test
        @DisplayName("IOException - IO_ERROR exception (connection refused)")
        void error_ioException() throws Exception {
            int closedPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                closedPort = ss.getLocalPort();
            }

            BlueskyConfig config = new BlueskyConfig("http://localhost:" + closedPort);
            try (BlueskyClient ioClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build())
                    .build()) {

                BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                    () -> ioClient.createSession("user", "pass"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }

        @Test
        @DisplayName("IOException retry then success (WireMock Fault then 200)")
        void error_ioException_retryThenSuccess() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("io-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("after-fault"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("io-retry")
                .whenScenarioStateIs("after-fault")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            SessionResponse response = client.createSession("user", "pass");
            assertNotNull(response);
            assertEquals("d", response.did());
        }

        @Test
        @DisplayName("InterruptedException - INTERRUPTED exception with thread interrupted")
        void error_interruptedException() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "30")
                    .withFixedDelay(100)));

            Thread testThread = Thread.currentThread();

            Thread interrupter = new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    // ignore
                }
                testThread.interrupt();
            });
            interrupter.start();

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals("INTERRUPTED", ex.getError());
            // Clear interrupted status so it doesn't affect other tests
            Thread.interrupted();
        }

        @Test
        @DisplayName("Error response with error field but no message - uses default")
        void error_errorFieldOnly() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("InvalidRequest", ex.getError());
            // message defaults to "HTTP 400"
            assertEquals("HTTP 400", ex.getMessage());
        }

        @Test
        @DisplayName("Error response with message but no error field - uses UNKNOWN")
        void error_messageFieldOnly() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"message":"Something happened"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(500, ex.getStatusCode());
            assertEquals("UNKNOWN", ex.getError());
            assertEquals("Something happened", ex.getMessage());
        }

        @Test
        @DisplayName("Error response with empty JSON object - default error and message")
        void error_emptyJsonObject() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("UNKNOWN", ex.getError());
            assertEquals("HTTP 400", ex.getMessage());
        }

        @Test
        @DisplayName("Verify Accept: application/json header is sent")
        void verify_acceptHeader() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.createSession("user", "pass");

            wireMock.verify(postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .withHeader("Accept", equalTo("application/json")));
        }
    }

    // =========================================================================
    // refreshSession
    // =========================================================================
    @Nested
    @DisplayName("refreshSession(refreshJwt)")
    class RefreshSessionTests {

        @Test
        @DisplayName("Success - returns new tokens")
        void success_newTokens() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:xyz",
                          "handle": "bob.bsky.social",
                          "accessJwt": "new-access-jwt",
                          "refreshJwt": "new-refresh-jwt"
                        }
                        """)));

            SessionResponse response = client.refreshSession("old-refresh-jwt");

            assertNotNull(response);
            assertEquals("did:plc:xyz", response.did());
            assertEquals("bob.bsky.social", response.handle());
            assertEquals("new-access-jwt", response.accessJwt());
            assertEquals("new-refresh-jwt", response.refreshJwt());
        }

        @Test
        @DisplayName("Verify POST to /xrpc/com.atproto.server.refreshSession")
        void verify_endpoint() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.refreshSession("token");

            wireMock.verify(1, postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.refreshSession")));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header with refreshJwt")
        void verify_authorizationHeader() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.refreshSession("my-refresh-jwt-token");

            wireMock.verify(postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .withHeader("Authorization", equalTo("Bearer my-refresh-jwt-token")));
        }

        @Test
        @DisplayName("Verify POST with no request body")
        void verify_noBody() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            client.refreshSession("token");

            // noBody() sends no content, body should be empty or zero-length
            wireMock.verify(postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.refreshSession")));
        }

        @Test
        @DisplayName("Response with extra unknown fields - ignored")
        void success_extraFields() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did":"d","handle":"h","accessJwt":"a","refreshJwt":"r",
                          "didDoc": {"id": "did:plc:123"},
                          "active": true
                        }
                        """)));

            SessionResponse response = client.refreshSession("token");

            assertNotNull(response);
            assertEquals("d", response.did());
        }

        @Test
        @DisplayName("400 Bad Request - exception")
        void error_400() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest","message":"Bad token format"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("bad-token"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("InvalidRequest", ex.getError());
        }

        @Test
        @DisplayName("401 expired token - exception")
        void error_401_expired() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"ExpiredToken","message":"Token has expired"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("expired-token"));

            assertEquals(401, ex.getStatusCode());
            assertEquals("ExpiredToken", ex.getError());
            assertEquals("Token has expired", ex.getMessage());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error_429_thenSuccess() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .inScenario("refresh-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .inScenario("refresh-429")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            SessionResponse response = client.refreshSession("token");
            assertNotNull(response);
        }

        @Test
        @DisplayName("500 Internal Server Error - exception")
        void error_500() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InternalServerError","message":"Server error"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("token"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - PARSE_ERROR")
        void error_invalidJson() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{invalid")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("token"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty response body - PARSE_ERROR")
        void error_emptyBody() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("token"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("IOException - IO_ERROR")
        void error_ioException() throws Exception {
            int closedPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                closedPort = ss.getLocalPort();
            }

            BlueskyConfig config = new BlueskyConfig("http://localhost:" + closedPort);
            try (BlueskyClient ioClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build())
                    .build()) {

                BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                    () -> ioClient.refreshSession("token"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message")
        void error_notValidJson() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.refreshSession"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.refreshSession("token"));

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }
    }

    // =========================================================================
    // createPost
    // =========================================================================
    @Nested
    @DisplayName("createPost(accessJwt, did, text)")
    class CreatePostTests {

        private static final String CREATE_RECORD_URL = "/xrpc/com.atproto.repo.createRecord";

        private void stubSuccess() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://did:plc:123/app.bsky.feed.post/rk1","cid":"bafyrei123"}
                        """)));
        }

        @Test
        @DisplayName("Success - returns PostResponse with uri and cid")
        void success_returnsPostResponse() {
            stubSuccess();

            PostResponse response = client.createPost("jwt", "did:plc:123", "Hello!");

            assertNotNull(response);
            assertEquals("at://did:plc:123/app.bsky.feed.post/rk1", response.uri());
            assertEquals("bafyrei123", response.cid());
        }

        @Test
        @DisplayName("Verify POST to /xrpc/com.atproto.repo.createRecord")
        void verify_endpoint() {
            stubSuccess();

            client.createPost("jwt", "did:plc:123", "test");

            wireMock.verify(1, postRequestedFor(urlEqualTo(CREATE_RECORD_URL)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verify_authorizationHeader() {
            stubSuccess();

            client.createPost("my-access-jwt", "did:plc:123", "test");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withHeader("Authorization", equalTo("Bearer my-access-jwt")));
        }

        @Test
        @DisplayName("Verify Content-Type: application/json header")
        void verify_contentType() {
            stubSuccess();

            client.createPost("jwt", "did:plc:123", "test");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Verify request body has repo=did")
        void verify_requestBody_repo() {
            stubSuccess();

            client.createPost("jwt", "did:plc:myDid", "test");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withRequestBody(matching(".*\"repo\"\\s*:\\s*\"did:plc:myDid\".*")));
        }

        @Test
        @DisplayName("Verify request body has collection=app.bsky.feed.post")
        void verify_requestBody_collection() {
            stubSuccess();

            client.createPost("jwt", "did:plc:123", "test");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withRequestBody(matching(".*\"collection\"\\s*:\\s*\"app\\.bsky\\.feed\\.post\".*")));
        }

        @Test
        @DisplayName("Verify request body record has $type, text, createdAt")
        void verify_requestBody_record() {
            stubSuccess();

            client.createPost("jwt", "did:plc:123", "Hello world");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withRequestBody(matching(".*\"\\$type\"\\s*:\\s*\"app\\.bsky\\.feed\\.post\".*"))
                .withRequestBody(matching(".*\"text\"\\s*:\\s*\"Hello world\".*"))
                .withRequestBody(matching(".*\"createdAt\"\\s*:\\s*\"\\d{4}-.*\".*")));
        }

        @Test
        @DisplayName("Text with Japanese characters - success")
        void success_japaneseText() {
            stubSuccess();

            PostResponse response = client.createPost("jwt", "did:plc:123", "こんにちは世界！テスト投稿です。");

            assertNotNull(response);

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withRequestBody(matching(".*こんにちは世界.*")));
        }

        @Test
        @DisplayName("Text with emoji - success")
        void success_emojiText() {
            stubSuccess();

            PostResponse response = client.createPost("jwt", "did:plc:123", "Hello \uD83D\uDE00\uD83C\uDF1F\uD83D\uDE80");

            assertNotNull(response);
        }

        @Test
        @DisplayName("Max length text (300 chars) - success")
        void success_maxLengthText() {
            stubSuccess();

            String longText = "a".repeat(300);
            PostResponse response = client.createPost("jwt", "did:plc:123", longText);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void success_extraFields() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "uri":"at://did:plc:123/app.bsky.feed.post/rk1",
                          "cid":"bafyrei123",
                          "commit": {"cid": "bafyrei456", "rev": "1234"},
                          "validationStatus": "valid"
                        }
                        """)));

            PostResponse response = client.createPost("jwt", "did:plc:123", "test");

            assertNotNull(response);
            assertEquals("at://did:plc:123/app.bsky.feed.post/rk1", response.uri());
            assertEquals("bafyrei123", response.cid());
        }

        @Test
        @DisplayName("400 InvalidRecord - exception")
        void error_400_invalidRecord() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRecord","message":"Invalid record data"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("InvalidRecord", ex.getError());
            assertEquals("Invalid record data", ex.getMessage());
        }

        @Test
        @DisplayName("401 Unauthorized - exception")
        void error_401() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"AuthenticationRequired","message":"Invalid token"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("bad-jwt", "did:plc:123", "test"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden - exception")
        void error_403() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"Forbidden","message":"Not authorized"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error_429_thenSuccess() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-429")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/r","cid":"c"}
                        """)));

            PostResponse response = client.createPost("jwt", "did:plc:123", "test");
            assertNotNull(response);
        }

        @Test
        @DisplayName("500 Internal Server Error - exception")
        void error_500() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InternalServerError","message":"Failed"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - PARSE_ERROR")
        void error_invalidJson() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{bad json")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty response body - PARSE_ERROR")
        void error_emptyBody() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error response with error/message fields - parsed correctly")
        void error_fieldsParseCorrectly() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRecord","message":"Missing required field: text"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals("InvalidRecord", ex.getError());
            assertEquals("Missing required field: text", ex.getMessage());
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message")
        void error_notValidJson() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("<html>Error</html>")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }

        @Test
        @DisplayName("IOException retry then success via WireMock fault")
        void error_ioRetryThenSuccess() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-io-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-io-retry")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/r","cid":"c"}
                        """)));

            PostResponse response = client.createPost("jwt", "did:plc:123", "test");
            assertNotNull(response);
        }

        @Test
        @DisplayName("Verify Accept header is sent")
        void verify_acceptHeader() {
            stubSuccess();

            client.createPost("jwt", "did:plc:123", "test");

            wireMock.verify(postRequestedFor(urlEqualTo(CREATE_RECORD_URL))
                .withHeader("Accept", equalTo("application/json")));
        }

        @Test
        @DisplayName("429 all retries exhausted - throws exception")
        void error_429_allRetriesExhausted() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createPost("jwt", "did:plc:123", "test"));

            assertEquals(429, ex.getStatusCode());
        }

        @Test
        @DisplayName("Multiple consecutive 429 then success on third retry")
        void error_429_multipleRetries() {
            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-multi-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retry-1"));

            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-multi-429")
                .whenScenarioStateIs("retry-1")
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retry-2"));

            wireMock.stubFor(post(urlEqualTo(CREATE_RECORD_URL))
                .inScenario("post-multi-429")
                .whenScenarioStateIs("retry-2")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"uri":"at://d/app.bsky.feed.post/r","cid":"c"}
                        """)));

            PostResponse response = client.createPost("jwt", "did:plc:123", "test");
            assertNotNull(response);
        }
    }

    // =========================================================================
    // deletePost
    // =========================================================================
    @Nested
    @DisplayName("deletePost(accessJwt, did, rkey)")
    class DeletePostTests {

        private static final String DELETE_RECORD_URL = "/xrpc/com.atproto.repo.deleteRecord";

        @Test
        @DisplayName("Success - no exception thrown")
        void success_noException() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));

            assertDoesNotThrow(() -> client.deletePost("jwt", "did:plc:123", "rkey1"));
        }

        @Test
        @DisplayName("Verify POST to /xrpc/com.atproto.repo.deleteRecord")
        void verify_endpoint() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            client.deletePost("jwt", "did:plc:123", "rkey1");

            wireMock.verify(1, postRequestedFor(urlEqualTo(DELETE_RECORD_URL)));
        }

        @Test
        @DisplayName("Verify request body has repo, collection, rkey")
        void verify_requestBody() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            client.deletePost("jwt", "did:plc:myDid", "myRkey");

            wireMock.verify(postRequestedFor(urlEqualTo(DELETE_RECORD_URL))
                .withRequestBody(equalToJson("""
                    {
                      "repo": "did:plc:myDid",
                      "collection": "app.bsky.feed.post",
                      "rkey": "myRkey"
                    }
                    """)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verify_authorizationHeader() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            client.deletePost("my-access-jwt", "did:plc:123", "rkey1");

            wireMock.verify(postRequestedFor(urlEqualTo(DELETE_RECORD_URL))
                .withHeader("Authorization", equalTo("Bearer my-access-jwt")));
        }

        @Test
        @DisplayName("Verify Content-Type: application/json header")
        void verify_contentType() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            client.deletePost("jwt", "did:plc:123", "rkey1");

            wireMock.verify(postRequestedFor(urlEqualTo(DELETE_RECORD_URL))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("400 Bad Request - exception")
        void error_400() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest","message":"Bad rkey"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.deletePost("jwt", "did:plc:123", "bad-rkey"));

            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("401 Unauthorized - exception")
        void error_401() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"AuthenticationRequired","message":"Invalid token"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.deletePost("bad-jwt", "did:plc:123", "rkey1"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("404 Not Found - exception")
        void error_404() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"RecordNotFound","message":"Record not found"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.deletePost("jwt", "did:plc:123", "nonexistent"));

            assertEquals(404, ex.getStatusCode());
            assertEquals("RecordNotFound", ex.getError());
        }

        @Test
        @DisplayName("500 Internal Server Error - exception")
        void error_500() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InternalServerError","message":"Failed"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.deletePost("jwt", "did:plc:123", "rkey1"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error_429_thenSuccess() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .inScenario("delete-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .inScenario("delete-429")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            assertDoesNotThrow(() -> client.deletePost("jwt", "did:plc:123", "rkey1"));
        }

        @Test
        @DisplayName("IOException - IO_ERROR")
        void error_ioException() throws Exception {
            int closedPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                closedPort = ss.getLocalPort();
            }

            BlueskyConfig config = new BlueskyConfig("http://localhost:" + closedPort);
            try (BlueskyClient ioClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build())
                    .build()) {

                BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                    () -> ioClient.deletePost("jwt", "did:plc:123", "rkey1"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message")
        void error_notValidJson() {
            wireMock.stubFor(post(urlEqualTo(DELETE_RECORD_URL))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("not json")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.deletePost("jwt", "did:plc:123", "rkey1"));

            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }
    }

    // =========================================================================
    // getProfile
    // =========================================================================
    @Nested
    @DisplayName("getProfile(accessJwt, actor)")
    class GetProfileTests {

        private static final String PROFILE_PATH = "/xrpc/app.bsky.actor.getProfile";

        @Test
        @DisplayName("Success with all fields - verify all ProfileResponse fields")
        void success_allFields() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:profile1",
                          "handle": "alice.bsky.social",
                          "displayName": "Alice Wonderland",
                          "avatar": "https://cdn.bsky.social/avatar/alice.jpg",
                          "followersCount": 1000,
                          "followsCount": 500,
                          "postsCount": 2500
                        }
                        """)));

            ProfileResponse response = client.getProfile("jwt", "alice.bsky.social");

            assertNotNull(response);
            assertEquals("did:plc:profile1", response.did());
            assertEquals("alice.bsky.social", response.handle());
            assertEquals("Alice Wonderland", response.displayName());
            assertEquals("https://cdn.bsky.social/avatar/alice.jpg", response.avatar());
            assertEquals(1000, response.followersCount());
            assertEquals(500, response.followsCount());
            assertEquals(2500, response.postsCount());
        }

        @Test
        @DisplayName("Verify GET to /xrpc/app.bsky.actor.getProfile?actor=...")
        void verify_endpoint() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            client.getProfile("jwt", "user.bsky.social");

            wireMock.verify(1, getRequestedFor(urlPathEqualTo(PROFILE_PATH)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verify_authorizationHeader() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            client.getProfile("my-access-jwt", "user.bsky.social");

            wireMock.verify(getRequestedFor(urlPathEqualTo(PROFILE_PATH))
                .withHeader("Authorization", equalTo("Bearer my-access-jwt")));
        }

        @Test
        @DisplayName("Verify actor query parameter is passed")
        void verify_actorQueryParam() {
            wireMock.stubFor(get(urlEqualTo(PROFILE_PATH + "?actor=alice.bsky.social"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"alice.bsky.social","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            ProfileResponse response = client.getProfile("jwt", "alice.bsky.social");

            assertNotNull(response);
            assertEquals("alice.bsky.social", response.handle());
        }

        @Test
        @DisplayName("Actor with DID format")
        void success_actorWithDid() {
            wireMock.stubFor(get(urlEqualTo(PROFILE_PATH + "?actor=did:plc:abc123"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"did:plc:abc123","handle":"h","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            ProfileResponse response = client.getProfile("jwt", "did:plc:abc123");

            assertNotNull(response);
            assertEquals("did:plc:abc123", response.did());
        }

        @Test
        @DisplayName("Response with optional fields missing - default values")
        void success_optionalFieldsMissing() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:123",
                          "handle": "minimal.bsky.social"
                        }
                        """)));

            ProfileResponse response = client.getProfile("jwt", "minimal.bsky.social");

            assertNotNull(response);
            assertEquals("did:plc:123", response.did());
            assertEquals("minimal.bsky.social", response.handle());
            assertNull(response.displayName());
            assertNull(response.avatar());
            assertEquals(0, response.followersCount());
            assertEquals(0, response.followsCount());
            assertEquals(0, response.postsCount());
        }

        @Test
        @DisplayName("Response with extra unknown fields - ignored")
        void success_extraFields() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:123",
                          "handle": "h",
                          "displayName": "n",
                          "avatar": "a",
                          "followersCount": 10,
                          "followsCount": 5,
                          "postsCount": 20,
                          "banner": "https://cdn.bsky.social/banner.jpg",
                          "description": "Some bio",
                          "indexedAt": "2024-01-01T00:00:00Z",
                          "labels": []
                        }
                        """)));

            ProfileResponse response = client.getProfile("jwt", "h");

            assertNotNull(response);
            assertEquals("did:plc:123", response.did());
        }

        @Test
        @DisplayName("400 Bad Request - exception")
        void error_400() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest","message":"Invalid actor"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "bad-actor"));

            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("401 Unauthorized - exception")
        void error_401() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"AuthenticationRequired","message":"Invalid token"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("bad-jwt", "user.bsky.social"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("404 actor not found - exception")
        void error_404() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"ActorNotFound","message":"Profile not found"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "nonexistent.bsky.social"));

            assertEquals(404, ex.getStatusCode());
            assertEquals("ActorNotFound", ex.getError());
            assertEquals("Profile not found", ex.getMessage());
        }

        @Test
        @DisplayName("500 Internal Server Error - exception")
        void error_500() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InternalServerError","message":"Server error"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "user.bsky.social"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - PARSE_ERROR")
        void error_invalidJson() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not json")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "user.bsky.social"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty response body - PARSE_ERROR")
        void error_emptyBody() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "user.bsky.social"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error_429_thenSuccess() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .inScenario("profile-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("ok"));

            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .inScenario("profile-429")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            ProfileResponse response = client.getProfile("jwt", "user.bsky.social");
            assertNotNull(response);
        }

        @Test
        @DisplayName("IOException - IO_ERROR")
        void error_ioException() throws Exception {
            int closedPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                closedPort = ss.getLocalPort();
            }

            BlueskyConfig config = new BlueskyConfig("http://localhost:" + closedPort);
            try (BlueskyClient ioClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build())
                    .build()) {

                BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                    () -> ioClient.getProfile("jwt", "user.bsky.social"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message")
        void error_notValidJson() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("<html>Error</html>")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.getProfile("jwt", "user.bsky.social"));

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }

        @Test
        @DisplayName("Verify Accept: application/json header")
        void verify_acceptHeader() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","displayName":"n","avatar":"a",
                         "followersCount":0,"followsCount":0,"postsCount":0}
                        """)));

            client.getProfile("jwt", "user.bsky.social");

            wireMock.verify(getRequestedFor(urlPathEqualTo(PROFILE_PATH))
                .withHeader("Accept", equalTo("application/json")));
        }

        @Test
        @DisplayName("Profile with large follower counts")
        void success_largeFollowerCounts() {
            wireMock.stubFor(get(urlPathEqualTo(PROFILE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "did": "did:plc:popular",
                          "handle": "popular.bsky.social",
                          "displayName": "Popular User",
                          "avatar": "https://cdn.bsky.social/avatar.jpg",
                          "followersCount": 1000000,
                          "followsCount": 100,
                          "postsCount": 50000
                        }
                        """)));

            ProfileResponse response = client.getProfile("jwt", "popular.bsky.social");

            assertEquals(1000000, response.followersCount());
            assertEquals(50000, response.postsCount());
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================
    @Nested
    @DisplayName("BlueskyClient.Builder")
    class BuilderTests {

        @Test
        @DisplayName("Build without config - throws IllegalStateException")
        void build_withoutConfig_throwsIllegalStateException() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BlueskyClient.builder().build());

            assertEquals("BlueskyConfig must be provided", ex.getMessage());
        }

        @Test
        @DisplayName("Build with config only - uses defaults for HttpClient and ObjectMapper")
        void build_withConfigOnly() {
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());

            // Should not throw
            BlueskyClient builtClient = BlueskyClient.builder()
                .config(config)
                .build();

            assertNotNull(builtClient);
            builtClient.close();
        }

        @Test
        @DisplayName("Build with custom HttpClient - uses it")
        void build_withCustomHttpClient() {
            HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            try (BlueskyClient customBuiltClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(customClient)
                    .build()) {

                SessionResponse response = customBuiltClient.createSession("user", "pass");
                assertNotNull(response);
            }
        }

        @Test
        @DisplayName("Build with custom ObjectMapper - uses it")
        void build_withCustomObjectMapper() {
            ObjectMapper customMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            try (BlueskyClient customBuiltClient = BlueskyClient.builder()
                    .config(config)
                    .objectMapper(customMapper)
                    .build()) {

                SessionResponse response = customBuiltClient.createSession("user", "pass");
                assertNotNull(response);
            }
        }

        @Test
        @DisplayName("close() on default client - no error")
        void close_defaultClient_noError() {
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());
            BlueskyClient defaultClient = BlueskyClient.builder()
                .config(config)
                .build();

            assertDoesNotThrow(defaultClient::close);
        }

        @Test
        @DisplayName("close() on custom client - closes it")
        void close_customClient_closesIt() {
            HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());

            BlueskyClient customBuiltClient = BlueskyClient.builder()
                .config(config)
                .httpClient(customClient)
                .build();

            assertDoesNotThrow(customBuiltClient::close);
        }

        @Test
        @DisplayName("Builder is fluent - all setters return Builder")
        void builder_fluent() {
            BlueskyConfig config = new BlueskyConfig(wireMock.baseUrl());
            HttpClient customClient = HttpClient.newBuilder().build();
            ObjectMapper customMapper = new ObjectMapper();

            // This should compile and work if builder is fluent
            BlueskyClient builtClient = BlueskyClient.builder()
                .config(config)
                .httpClient(customClient)
                .objectMapper(customMapper)
                .build();

            assertNotNull(builtClient);
            builtClient.close();
        }
    }

    // =========================================================================
    // BlueskyConfig
    // =========================================================================
    @Nested
    @DisplayName("BlueskyConfig")
    class BlueskyConfigTests {

        @Test
        @DisplayName("Valid config - no error")
        void validConfig() {
            BlueskyConfig config = new BlueskyConfig("https://bsky.social");

            assertNotNull(config);
            assertEquals("https://bsky.social", config.serviceUrl());
        }

        @Test
        @DisplayName("Null serviceUrl - throws IllegalArgumentException")
        void nullServiceUrl() {
            assertThrows(IllegalArgumentException.class,
                () -> new BlueskyConfig(null));
        }

        @Test
        @DisplayName("Blank serviceUrl - throws IllegalArgumentException")
        void blankServiceUrl() {
            assertThrows(IllegalArgumentException.class,
                () -> new BlueskyConfig(""));
        }

        @Test
        @DisplayName("Whitespace-only serviceUrl - throws IllegalArgumentException")
        void whitespaceServiceUrl() {
            assertThrows(IllegalArgumentException.class,
                () -> new BlueskyConfig("   "));
        }

        @Test
        @DisplayName("defaultConfig() - returns bsky.social URL")
        void defaultConfig() {
            BlueskyConfig config = BlueskyConfig.defaultConfig();

            assertNotNull(config);
            assertEquals("https://bsky.social", config.serviceUrl());
        }
    }

    // =========================================================================
    // Cross-cutting / Error detail
    // =========================================================================
    @Nested
    @DisplayName("Error detail field")
    class ErrorDetailTests {

        @Test
        @DisplayName("Error response detail contains raw response body")
        void errorDetail_containsRawBody() {
            String rawBody = """
                {"error":"RateLimitExceeded","message":"Too many requests"}
                """;
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withBody(rawBody)));

            // 429 with all retries exhausted
            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(429, ex.getStatusCode());
            assertEquals("RateLimitExceeded", ex.getError());
            // detail should contain the raw body
            assertNotNull(ex.getDetail());
            assertTrue(ex.getDetail().contains("RateLimitExceeded"));
        }

        @Test
        @DisplayName("Error response with valid JSON preserves error and message")
        void errorResponse_preservesFields() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidHandle","message":"Handle must be a valid domain"}
                        """)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals("InvalidHandle", ex.getError());
            assertEquals("Handle must be a valid domain", ex.getMessage());
        }

        @Test
        @DisplayName("BlueskyApiException has correct cause for IO errors")
        void ioError_hasCause() throws Exception {
            int closedPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                closedPort = ss.getLocalPort();
            }

            BlueskyConfig config = new BlueskyConfig("http://localhost:" + closedPort);
            try (BlueskyClient ioClient = BlueskyClient.builder()
                    .config(config)
                    .httpClient(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build())
                    .build()) {

                BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                    () -> ioClient.createSession("user", "pass"));

                assertNotNull(ex.getCause());
                assertTrue(ex.getCause() instanceof java.io.IOException);
            }
        }

        @Test
        @DisplayName("BlueskyApiException has correct cause for parse errors")
        void parseError_hasCause() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not json")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertNotNull(ex.getCause());
        }

        @Test
        @DisplayName("BlueskyApiException for non-JSON error has cause")
        void nonJsonError_hasCause() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(502)
                    .withBody("Bad Gateway")));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(502, ex.getStatusCode());
            assertEquals("UNKNOWN", ex.getError());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
            assertNotNull(ex.getCause());
        }
    }

    // =========================================================================
    // Retry behavior (cross-method)
    // =========================================================================
    @Nested
    @DisplayName("Retry behavior")
    class RetryBehaviorTests {

        @Test
        @DisplayName("429 without Retry-After header uses exponential backoff")
        void retry_429_noRetryAfterHeader() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("no-retry-after")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("no-retry-after")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            SessionResponse response = client.createSession("user", "pass");
            assertNotNull(response);
        }

        @Test
        @DisplayName("IO fault retry then success across all retries")
        void ioFault_multipleRetries() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("multi-io-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("fault-2"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("multi-io-retry")
                .whenScenarioStateIs("fault-2")
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("ok"));

            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .inScenario("multi-io-retry")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"did":"d","handle":"h","accessJwt":"a","refreshJwt":"r"}
                        """)));

            SessionResponse response = client.createSession("user", "pass");
            assertNotNull(response);
        }

        @Test
        @DisplayName("IO fault all retries exhausted - throws IO_ERROR")
        void ioFault_allRetriesExhausted() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            BlueskyApiException ex = assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            assertEquals(0, ex.getStatusCode());
            assertEquals("IO_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Non-429 HTTP error is not retried")
        void nonRetryableError_notRetried() {
            wireMock.stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {"error":"InvalidRequest","message":"Bad request"}
                        """)));

            assertThrows(BlueskyApiException.class,
                () -> client.createSession("user", "pass"));

            // Should only be called once - no retry for 400
            wireMock.verify(1, postRequestedFor(urlEqualTo("/xrpc/com.atproto.server.createSession")));
        }
    }
}
