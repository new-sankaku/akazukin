package com.akazukin.sdk.reddit;

import com.akazukin.sdk.reddit.exception.RedditApiException;
import com.akazukin.sdk.reddit.model.RedditUser;
import com.akazukin.sdk.reddit.model.SubmitResponse;
import com.akazukin.sdk.reddit.model.TokenResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedditClientWireMockTest {

    private WireMockServer wireMockServer;
    private RedditClient client;

    private static final String CLIENT_ID = "testClientId";
    private static final String CLIENT_SECRET = "testClientSecret";
    private static final String REDIRECT_URI = "http://localhost/callback";
    private static final String USER_AGENT = "TestAgent/1.0";
    private static final String TOKEN_PATH = "/api/v1/access_token";
    private static final String SUBMIT_PATH = "/api/submit";
    private static final String DELETE_PATH = "/api/del";
    private static final String ME_PATH = "/api/v1/me";

    private static final String TOKEN_RESPONSE_JSON =
        "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"scope\":\"read\"}";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        String baseUrl = "http://localhost:" + wireMockServer.port();

        RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);
        client = RedditClient.builder()
            .config(config)
            .apiBaseUrl(baseUrl)
            .tokenUrl(baseUrl + TOKEN_PATH)
            .authBaseUrl(baseUrl + "/api/v1/authorize")
            .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private String expectedBasicAuth() {
        return Base64.getEncoder()
            .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
    }

    private void stubTokenSuccess() {
        wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TOKEN_RESPONSE_JSON)));
    }

    // ========================================================================================
    // exchangeToken
    // ========================================================================================

    @Nested
    @DisplayName("exchangeToken")
    class ExchangeToken {

        @Test
        @DisplayName("Success - returns all TokenResponse fields")
        void success_returnsAllFields() {
            stubTokenSuccess();

            TokenResponse response = client.exchangeToken("auth_code_123");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
            assertEquals("rt", response.refreshToken());
            assertEquals(3600, response.expiresIn());
            assertEquals("read", response.scope());
        }

        @Test
        @DisplayName("Verify POST method is used")
        void verifyPostMethod() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }

        @Test
        @DisplayName("Verify URL is the token endpoint")
        void verifyTokenEndpointUrl() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH)));
        }

        @Test
        @DisplayName("Verify Content-Type is x-www-form-urlencoded")
        void verifyContentType() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
        }

        @Test
        @DisplayName("Verify Authorization: Basic header = Base64(clientId:clientSecret)")
        void verifyBasicAuthHeader() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withHeader("Authorization", equalTo("Basic " + expectedBasicAuth())));
        }

        @Test
        @DisplayName("Verify User-Agent header matches config")
        void verifyUserAgentHeader() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
        }

        @Test
        @DisplayName("Verify body has grant_type=authorization_code")
        void verifyGrantType() {
            stubTokenSuccess();

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("grant_type=authorization_code")));
        }

        @Test
        @DisplayName("Verify body has code parameter")
        void verifyCodeParameter() {
            stubTokenSuccess();

            client.exchangeToken("my_auth_code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("code=my_auth_code")));
        }

        @Test
        @DisplayName("Verify body has redirect_uri parameter")
        void verifyRedirectUri() {
            stubTokenSuccess();

            client.exchangeToken("code");

            String encodedUri = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("redirect_uri=" + encodedUri)));
        }

        @Test
        @DisplayName("Code with special characters is properly URL-encoded")
        void codeWithSpecialCharsEncoded() {
            stubTokenSuccess();

            client.exchangeToken("code with spaces&special=chars");

            String encoded = URLEncoder.encode("code with spaces&special=chars", StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("code=" + encoded)));
        }

        @Test
        @DisplayName("400 Bad Request throws RedditApiException")
        void error400() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_grant\",\"message\":\"Invalid authorization code\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("bad_code"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("invalid_grant", ex.getError());
            assertEquals("Invalid authorization code", ex.getMessage());
        }

        @Test
        @DisplayName("401 Unauthorized with error and message parsed")
        void error401WithParsedFields() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_client\",\"message\":\"Client authentication failed\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(401, ex.getStatusCode());
            assertEquals("invalid_client", ex.getError());
            assertEquals("Client authentication failed", ex.getMessage());
        }

        @Test
        @DisplayName("403 Forbidden throws RedditApiException")
        void error403() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"forbidden\",\"message\":\"Access denied\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(403, ex.getStatusCode());
            assertEquals("forbidden", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("429-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("429-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TOKEN_RESPONSE_JSON)));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
        }

        @Test
        @DisplayName("429 with Retry-After header - respects wait")
        void error429WithRetryAfterHeader() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("429-retry-after")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("429-retry-after")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TOKEN_RESPONSE_JSON)));

            long start = System.currentTimeMillis();
            TokenResponse response = client.exchangeToken("code");
            long elapsed = System.currentTimeMillis() - start;

            assertNotNull(response);
            assertTrue(elapsed >= 900, "Should have waited at least ~1 second for Retry-After");
        }

        @Test
        @DisplayName("429 all retries fail - throws exception")
        void error429AllRetriesFail() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(429, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"server_error\",\"message\":\"Internal error\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Empty response body results in PARSE_ERROR")
        void emptyResponseBody() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Invalid JSON response results in PARSE_ERROR")
        void invalidJsonResponse() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not-json-at-all")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error JSON with 'error' field parsed correctly")
        void errorJsonWithErrorField() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"unsupported_grant_type\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("unsupported_grant_type", ex.getError());
        }

        @Test
        @DisplayName("Error JSON with 'error' + 'error_description' uses error_description as message")
        void errorJsonWithErrorDescription() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_grant\","
                        + "\"error_description\":\"The provided authorization grant is invalid\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("invalid_grant", ex.getError());
            assertEquals("The provided authorization grant is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("Error JSON with 'message' takes priority over 'error_description'")
        void errorJsonMessagePriorityOverDescription() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad_request\","
                        + "\"message\":\"Primary message\","
                        + "\"error_description\":\"Secondary description\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("Primary message", ex.getMessage());
        }

        @Test
        @DisplayName("Error with invalid JSON body has fallback message with '(response body not valid JSON)'")
        void errorWithInvalidJsonFallback() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("this is not json")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(500, ex.getStatusCode());
            assertEquals("UNKNOWN", ex.getError());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }

        @Test
        @DisplayName("IOException triggers IO_ERROR")
        void ioExceptionTriggersIoError() {
            // Use a port that nobody is listening on to cause IOException
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);
            try (RedditClient badClient = RedditClient.builder()
                    .config(config)
                    .tokenUrl("http://localhost:1/api/v1/access_token")
                    .apiBaseUrl("http://localhost:1")
                    .build()) {

                RedditApiException ex = assertThrows(RedditApiException.class,
                    () -> badClient.exchangeToken("code"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }

        @Test
        @DisplayName("IOException then success - retry works")
        void ioExceptionThenSuccess() {
            // First call -> connection refused (WireMock not yet stubbed to cause fault)
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("io-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("io-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TOKEN_RESPONSE_JSON)));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
        }
    }

    // ========================================================================================
    // refreshToken
    // ========================================================================================

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("Success - returns all TokenResponse fields")
        void success() {
            String json = "{\"access_token\":\"new_at\",\"refresh_token\":\"new_rt\","
                + "\"expires_in\":7200,\"scope\":\"read write\"}";
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            TokenResponse response = client.refreshToken("old_refresh_token");

            assertNotNull(response);
            assertEquals("new_at", response.accessToken());
            assertEquals("new_rt", response.refreshToken());
            assertEquals(7200, response.expiresIn());
            assertEquals("read write", response.scope());
        }

        @Test
        @DisplayName("Verify body has grant_type=refresh_token")
        void verifyGrantType() {
            stubTokenSuccess();

            client.refreshToken("rt");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("grant_type=refresh_token")));
        }

        @Test
        @DisplayName("Verify body has refresh_token parameter")
        void verifyRefreshTokenParam() {
            stubTokenSuccess();

            client.refreshToken("my_refresh_token_value");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withRequestBody(containing("refresh_token=my_refresh_token_value")));
        }

        @Test
        @DisplayName("Verify Basic auth header")
        void verifyBasicAuth() {
            stubTokenSuccess();

            client.refreshToken("rt");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withHeader("Authorization", equalTo("Basic " + expectedBasicAuth())));
        }

        @Test
        @DisplayName("Verify User-Agent header")
        void verifyUserAgent() {
            stubTokenSuccess();

            client.refreshToken("rt");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
        }

        @Test
        @DisplayName("400 invalid_grant throws exception")
        void error400InvalidGrant() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_grant\",\"message\":\"Refresh token expired\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.refreshToken("expired_rt"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("invalid_grant", ex.getError());
        }

        @Test
        @DisplayName("401 Unauthorized throws exception")
        void error401() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_client\",\"message\":\"Bad client credentials\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{\"error\":\"server_error\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response results in PARSE_ERROR")
        void invalidJsonParseError() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("<html>not json</html>")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Response with null refresh_token field")
        void nullRefreshTokenInResponse() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"new_at\",\"expires_in\":3600,\"scope\":\"read\"}")));

            TokenResponse response = client.refreshToken("rt");

            assertNotNull(response);
            assertEquals("new_at", response.accessToken());
            assertNull(response.refreshToken());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("refresh-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .inScenario("refresh-429")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TOKEN_RESPONSE_JSON)));

            TokenResponse response = client.refreshToken("rt");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
        }

        @Test
        @DisplayName("Response with extra fields - ignored gracefully")
        void extraFieldsIgnored() {
            wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\","
                        + "\"expires_in\":3600,\"scope\":\"read\","
                        + "\"unknown_field\":\"some_value\",\"token_type\":\"bearer\"}")));

            TokenResponse response = client.refreshToken("rt");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
        }
    }

    // ========================================================================================
    // submitPost
    // ========================================================================================

    @Nested
    @DisplayName("submitPost")
    class SubmitPost {

        private static final String SUBMIT_SUCCESS_JSON =
            "{\"json\":{\"data\":{\"id\":\"abc123\",\"name\":\"t3_abc123\","
                + "\"url\":\"https://reddit.com/r/test/comments/abc123\"},\"errors\":[]}}";

        private void stubSubmitSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(SUBMIT_SUCCESS_JSON)));
        }

        @Test
        @DisplayName("Success - verify all SubmitResponse fields")
        void success() {
            stubSubmitSuccess();

            SubmitResponse response = client.submitPost("token", "test", "Title", "Body text");

            assertNotNull(response);
            assertEquals("abc123", response.id());
            assertEquals("t3_abc123", response.name());
            assertEquals("https://reddit.com/r/test/comments/abc123", response.url());
        }

        @Test
        @DisplayName("Verify POST to /api/submit")
        void verifyPostMethod() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verifyBearerAuth() {
            stubSubmitSuccess();

            client.submitPost("my_access_token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withHeader("Authorization", equalTo("Bearer my_access_token")));
        }

        @Test
        @DisplayName("Verify Content-Type: x-www-form-urlencoded")
        void verifyContentType() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
        }

        @Test
        @DisplayName("Verify User-Agent header")
        void verifyUserAgent() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
        }

        @Test
        @DisplayName("Verify body has api_type=json")
        void verifyApiType() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("api_type=json")));
        }

        @Test
        @DisplayName("Verify body has kind=self")
        void verifyKind() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("kind=self")));
        }

        @Test
        @DisplayName("Verify body has sr (subreddit) parameter")
        void verifySubreddit() {
            stubSubmitSuccess();

            client.submitPost("token", "programming", "t", "b");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("sr=programming")));
        }

        @Test
        @DisplayName("Verify body has title parameter")
        void verifyTitle() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "My Post Title", "b");

            String encodedTitle = URLEncoder.encode("My Post Title", StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("title=" + encodedTitle)));
        }

        @Test
        @DisplayName("Verify body has text parameter")
        void verifyText() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "Hello world body");

            String encodedText = URLEncoder.encode("Hello world body", StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("text=" + encodedText)));
        }

        @Test
        @DisplayName("Title with special characters is URL-encoded")
        void titleWithSpecialChars() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "Hello & Goodbye <World>", "b");

            String encoded = URLEncoder.encode("Hello & Goodbye <World>", StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("title=" + encoded)));
        }

        @Test
        @DisplayName("Text with newlines is URL-encoded")
        void textWithNewlines() {
            stubSubmitSuccess();

            client.submitPost("token", "sub", "t", "Line1\nLine2\nLine3");

            String encoded = URLEncoder.encode("Line1\nLine2\nLine3", StandardCharsets.UTF_8);
            wireMockServer.verify(postRequestedFor(urlPathEqualTo(SUBMIT_PATH))
                .withRequestBody(containing("text=" + encoded)));
        }

        @Test
        @DisplayName("Long text (close to Reddit limit) succeeds")
        void longText() {
            stubSubmitSuccess();

            String longText = "x".repeat(10000);
            SubmitResponse response = client.submitPost("token", "sub", "t", longText);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void responseExtraFieldsIgnored() {
            String json = "{\"json\":{\"data\":{\"id\":\"abc\",\"name\":\"t3_abc\","
                + "\"url\":\"https://reddit.com/r/t/abc\",\"drafts_count\":0,"
                + "\"websocket_url\":\"wss://example.com\"},\"errors\":[]}}";

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            SubmitResponse response = client.submitPost("token", "sub", "t", "b");

            assertEquals("abc", response.id());
        }

        @Test
        @DisplayName("API-level error in json.errors array throws RedditApiException with error code")
        void apiErrorInResponseErrors() {
            String json = "{\"json\":{\"errors\":[[\"ALREADY_SUB\","
                + "\"that link has already been submitted\",\"url\"]],\"data\":{}}}";

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals("ALREADY_SUB", ex.getError());
            assertEquals("that link has already been submitted", ex.getMessage());
        }

        @Test
        @DisplayName("API-level error with multiple errors uses first")
        void apiErrorMultipleUsesFirst() {
            String json = "{\"json\":{\"errors\":["
                + "[\"RATE_LIMIT\",\"you are doing that too much\",\"ratelimit\"],"
                + "[\"OTHER_ERROR\",\"another issue\",\"field\"]"
                + "],\"data\":{}}}";

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals("RATE_LIMIT", ex.getError());
            assertEquals("you are doing that too much", ex.getMessage());
        }

        @Test
        @DisplayName("API-level error with single-element array (no message) uses empty string")
        void apiErrorSingleElement() {
            String json = "{\"json\":{\"errors\":[[\"NO_TEXT\"]],\"data\":{}}}";

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals("NO_TEXT", ex.getError());
            assertEquals("", ex.getMessage());
        }

        @Test
        @DisplayName("json.errors is empty array - success (no error)")
        void emptyErrorsArraySuccess() {
            stubSubmitSuccess();

            SubmitResponse response = client.submitPost("token", "sub", "t", "b");

            assertNotNull(response);
            assertEquals("abc123", response.id());
        }

        @Test
        @DisplayName("400 Bad Request throws exception")
        void httpError400() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad_request\",\"message\":\"Invalid parameters\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("401 Unauthorized throws exception")
        void httpError401() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"error\":\"invalid_token\",\"message\":\"Token expired\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden throws exception (subreddit restricted)")
        void httpError403() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withBody("{\"error\":\"forbidden\",\"message\":\"Restricted subreddit\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "restricted_sub", "t", "b"));

            assertEquals(403, ex.getStatusCode());
            assertEquals("forbidden", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void httpError429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .inScenario("submit-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .inScenario("submit-429")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(SUBMIT_SUCCESS_JSON)));

            SubmitResponse response = client.submitPost("token", "sub", "t", "b");

            assertNotNull(response);
            assertEquals("abc123", response.id());
        }

        @Test
        @DisplayName("500 Internal Server Error throws exception")
        void httpError500() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{\"error\":\"server_error\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON in 200 response results in PARSE_ERROR")
        void invalidJsonParseError() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("<<<not json>>>")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error response with 'error' field in JSON is parsed")
        void errorJsonParsed() {
            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"QUOTA_FILLED\",\"message\":\"Post quota exceeded\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.submitPost("token", "sub", "t", "b"));

            assertEquals("QUOTA_FILLED", ex.getError());
            assertEquals("Post quota exceeded", ex.getMessage());
        }

        @Test
        @DisplayName("json.data with null fields returns SubmitResponse with null values")
        void dataWithNullFields() {
            String json = "{\"json\":{\"data\":{},\"errors\":[]}}";

            wireMockServer.stubFor(post(urlPathEqualTo(SUBMIT_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            SubmitResponse response = client.submitPost("token", "sub", "t", "b");

            assertNull(response.id());
            assertNull(response.name());
            assertNull(response.url());
        }
    }

    // ========================================================================================
    // deletePost
    // ========================================================================================

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        private void stubDeleteSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}")));
        }

        @Test
        @DisplayName("Success - no exception thrown")
        void success() {
            stubDeleteSuccess();

            assertDoesNotThrow(() -> client.deletePost("token", "t3_abc123"));
        }

        @Test
        @DisplayName("Verify POST to /api/del")
        void verifyPostToApiDel() {
            stubDeleteSuccess();

            client.deletePost("token", "t3_abc123");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(DELETE_PATH)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verifyBearerAuth() {
            stubDeleteSuccess();

            client.deletePost("my_token", "t3_abc123");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(DELETE_PATH))
                .withHeader("Authorization", equalTo("Bearer my_token")));
        }

        @Test
        @DisplayName("Verify body has id parameter")
        void verifyIdParameter() {
            stubDeleteSuccess();

            client.deletePost("token", "t3_abc123");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(DELETE_PATH))
                .withRequestBody(containing("id=t3_abc123")));
        }

        @Test
        @DisplayName("Verify User-Agent header")
        void verifyUserAgent() {
            stubDeleteSuccess();

            client.deletePost("token", "t3_abc123");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(DELETE_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
        }

        @Test
        @DisplayName("Verify Content-Type header")
        void verifyContentType() {
            stubDeleteSuccess();

            client.deletePost("token", "t3_abc123");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo(DELETE_PATH))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
        }

        @Test
        @DisplayName("400 Bad Request throws exception")
        void error400() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withBody("{\"error\":\"bad_request\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.deletePost("token", "invalid_id"));

            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("401 Unauthorized throws exception")
        void error401() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"error\":\"invalid_token\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.deletePost("expired_token", "t3_abc123"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden throws exception")
        void error403() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withBody("{\"error\":\"forbidden\",\"message\":\"Not your post\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.deletePost("token", "t3_other"));

            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{\"error\":\"server_error\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.deletePost("token", "t3_abc123"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .inScenario("delete-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .inScenario("delete-429")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{}")));

            assertDoesNotThrow(() -> client.deletePost("token", "t3_abc123"));
        }

        @Test
        @DisplayName("Error response with invalid JSON uses fallback message")
        void errorInvalidJsonFallback() {
            wireMockServer.stubFor(post(urlPathEqualTo(DELETE_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.deletePost("token", "t3_abc123"));

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
        }
    }

    // ========================================================================================
    // getMe
    // ========================================================================================

    @Nested
    @DisplayName("getMe")
    class GetMe {

        private static final String ME_SUCCESS_JSON =
            "{\"name\":\"testuser\",\"id\":\"t2_xyz\","
                + "\"link_karma\":1000,\"comment_karma\":2000,"
                + "\"icon_img\":\"https://www.redditstatic.com/avatars/testuser.png\"}";

        private void stubMeSuccess() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ME_SUCCESS_JSON)));
        }

        @Test
        @DisplayName("Success with all fields - verify all RedditUser fields")
        void successAllFields() {
            stubMeSuccess();

            RedditUser user = client.getMe("token");

            assertNotNull(user);
            assertEquals("testuser", user.name());
            assertEquals("t2_xyz", user.id());
            assertEquals(1000, user.linkKarma());
            assertEquals(2000, user.commentKarma());
            assertEquals("https://www.redditstatic.com/avatars/testuser.png", user.iconImg());
        }

        @Test
        @DisplayName("Verify GET to /api/v1/me")
        void verifyGetMethod() {
            stubMeSuccess();

            client.getMe("token");

            wireMockServer.verify(getRequestedFor(urlPathEqualTo(ME_PATH)));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer header")
        void verifyBearerAuth() {
            stubMeSuccess();

            client.getMe("my_access_token");

            wireMockServer.verify(getRequestedFor(urlPathEqualTo(ME_PATH))
                .withHeader("Authorization", equalTo("Bearer my_access_token")));
        }

        @Test
        @DisplayName("Verify User-Agent header")
        void verifyUserAgent() {
            stubMeSuccess();

            client.getMe("token");

            wireMockServer.verify(getRequestedFor(urlPathEqualTo(ME_PATH))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void extraFieldsIgnored() {
            String json = "{\"name\":\"testuser\",\"id\":\"t2_xyz\","
                + "\"link_karma\":100,\"comment_karma\":200,"
                + "\"icon_img\":\"https://example.com/img.png\","
                + "\"has_mail\":false,\"over_18\":true,\"created_utc\":1234567890.0}";

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditUser user = client.getMe("token");

            assertEquals("testuser", user.name());
            assertEquals("t2_xyz", user.id());
        }

        @Test
        @DisplayName("Zero karma values returned correctly")
        void zeroKarmaValues() {
            String json = "{\"name\":\"newuser\",\"id\":\"t2_new\","
                + "\"link_karma\":0,\"comment_karma\":0,\"icon_img\":null}";

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditUser user = client.getMe("token");

            assertEquals(0, user.linkKarma());
            assertEquals(0, user.commentKarma());
        }

        @Test
        @DisplayName("Missing optional fields use defaults")
        void missingOptionalFieldsDefaults() {
            String json = "{\"name\":\"minimal\",\"id\":\"t2_min\"}";

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditUser user = client.getMe("token");

            assertEquals("minimal", user.name());
            assertEquals("t2_min", user.id());
            assertEquals(0, user.linkKarma());
            assertEquals(0, user.commentKarma());
            assertNull(user.iconImg());
        }

        @Test
        @DisplayName("401 Unauthorized throws exception")
        void error401() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"error\":\"invalid_token\",\"message\":\"Token expired\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("expired_token"));

            assertEquals(401, ex.getStatusCode());
            assertEquals("invalid_token", ex.getError());
        }

        @Test
        @DisplayName("403 Forbidden throws exception")
        void error403() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withBody("{\"error\":\"forbidden\",\"message\":\"Insufficient scope\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("token"));

            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error throws exception")
        void error500() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("{\"error\":\"server_error\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("token"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response results in PARSE_ERROR")
        void invalidJsonParseError() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{invalid json content")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("token"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty response body results in PARSE_ERROR")
        void emptyResponseBody() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("token"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .inScenario("me-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .inScenario("me-429")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ME_SUCCESS_JSON)));

            RedditUser user = client.getMe("token");

            assertNotNull(user);
            assertEquals("testuser", user.name());
        }

        @Test
        @DisplayName("Negative karma values returned correctly")
        void negativeKarmaValues() {
            String json = "{\"name\":\"troll\",\"id\":\"t2_trl\","
                + "\"link_karma\":-100,\"comment_karma\":-500,\"icon_img\":null}";

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditUser user = client.getMe("token");

            assertEquals(-100, user.linkKarma());
            assertEquals(-500, user.commentKarma());
        }

        @Test
        @DisplayName("Large karma values returned correctly")
        void largeKarmaValues() {
            String json = "{\"name\":\"popular\",\"id\":\"t2_pop\","
                + "\"link_karma\":999999999,\"comment_karma\":888888888,\"icon_img\":null}";

            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json)));

            RedditUser user = client.getMe("token");

            assertEquals(999999999, user.linkKarma());
            assertEquals(888888888, user.commentKarma());
        }

        @Test
        @DisplayName("Error with error_description field uses it as message")
        void errorWithErrorDescription() {
            wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withBody("{\"error\":\"invalid_token\","
                        + "\"error_description\":\"The access token is invalid\"}")));

            RedditApiException ex = assertThrows(RedditApiException.class,
                () -> client.getMe("bad_token"));

            assertEquals("The access token is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("IOException triggers IO_ERROR")
        void ioErrorOnConnectionFailure() {
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);
            try (RedditClient badClient = RedditClient.builder()
                    .config(config)
                    .apiBaseUrl("http://localhost:1")
                    .tokenUrl("http://localhost:1" + TOKEN_PATH)
                    .build()) {

                RedditApiException ex = assertThrows(RedditApiException.class,
                    () -> badClient.getMe("token"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            }
        }
    }

    // ========================================================================================
    // getAuthorizationUrl
    // ========================================================================================

    @Nested
    @DisplayName("getAuthorizationUrl")
    class GetAuthorizationUrl {

        @Test
        @DisplayName("Contains client_id")
        void containsClientId() {
            String url = client.getAuthorizationUrl("state123", List.of("read"));

            assertTrue(url.contains("client_id=" + CLIENT_ID));
        }

        @Test
        @DisplayName("Contains response_type=code")
        void containsResponseTypeCode() {
            String url = client.getAuthorizationUrl("state123", List.of("read"));

            assertTrue(url.contains("response_type=code"));
        }

        @Test
        @DisplayName("Contains state parameter")
        void containsState() {
            String url = client.getAuthorizationUrl("my_unique_state", List.of("read"));

            assertTrue(url.contains("state=my_unique_state"));
        }

        @Test
        @DisplayName("Contains redirect_uri")
        void containsRedirectUri() {
            String url = client.getAuthorizationUrl("state", List.of("read"));

            String encodedUri = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
            assertTrue(url.contains("redirect_uri=" + encodedUri));
        }

        @Test
        @DisplayName("Contains duration=permanent")
        void containsDurationPermanent() {
            String url = client.getAuthorizationUrl("state", List.of("read"));

            assertTrue(url.contains("duration=permanent"));
        }

        @Test
        @DisplayName("Contains scope joined with commas")
        void containsScopeJoinedWithCommas() {
            String url = client.getAuthorizationUrl("state", List.of("read"));

            assertTrue(url.contains("scope=read"));
        }

        @Test
        @DisplayName("Multiple scopes joined with comma-separated encoding")
        void multipleScopesCommaSeparated() {
            String url = client.getAuthorizationUrl("state", List.of("read", "submit", "identity"));

            String encodedScope = URLEncoder.encode("read,submit,identity", StandardCharsets.UTF_8);
            assertTrue(url.contains("scope=" + encodedScope));
        }

        @Test
        @DisplayName("Special characters in state are URL-encoded")
        void stateSpecialCharsEncoded() {
            String url = client.getAuthorizationUrl("state with spaces&special=chars", List.of("read"));

            String encodedState = URLEncoder.encode("state with spaces&special=chars", StandardCharsets.UTF_8);
            assertTrue(url.contains("state=" + encodedState));
        }

        @Test
        @DisplayName("URL starts with auth base URL")
        void urlStartsWithAuthBase() {
            String url = client.getAuthorizationUrl("state", List.of("read"));

            assertTrue(url.startsWith("http://localhost:" + wireMockServer.port() + "/api/v1/authorize?"));
        }

        @Test
        @DisplayName("Single scope does not contain commas")
        void singleScopeNoComma() {
            String url = client.getAuthorizationUrl("state", List.of("identity"));

            assertTrue(url.contains("scope=identity"));
        }
    }

    // ========================================================================================
    // Builder
    // ========================================================================================

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Build without config throws IllegalStateException")
        void buildWithoutConfig() {
            assertThrows(IllegalStateException.class,
                () -> RedditClient.builder().build());
        }

        @Test
        @DisplayName("Build with config only uses defaults")
        void buildWithConfigUsesDefaults() {
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);

            RedditClient builtClient = RedditClient.builder().config(config).build();

            assertNotNull(builtClient);
            builtClient.close();
        }

        @Test
        @DisplayName("close() on default client does not throw")
        void closeDefaultNoError() {
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);
            RedditClient builtClient = RedditClient.builder().config(config).build();

            assertDoesNotThrow(builtClient::close);
        }

        @Test
        @DisplayName("close() on custom HttpClient closes it")
        void closeCustomHttpClient() {
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);
            HttpClient customClient = HttpClient.newBuilder().build();

            RedditClient builtClient = RedditClient.builder()
                .config(config)
                .httpClient(customClient)
                .build();

            assertDoesNotThrow(builtClient::close);
        }

        @Test
        @DisplayName("basicAuth() with null clientSecret encodes 'clientId:' (empty secret)")
        void basicAuthNullClientSecret() {
            RedditConfig config = new RedditConfig("myId", null, REDIRECT_URI, USER_AGENT);
            String baseUrl = "http://localhost:" + wireMockServer.port();

            try (RedditClient nullSecretClient = RedditClient.builder()
                    .config(config)
                    .apiBaseUrl(baseUrl)
                    .tokenUrl(baseUrl + TOKEN_PATH)
                    .build()) {

                wireMockServer.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TOKEN_RESPONSE_JSON)));

                nullSecretClient.exchangeToken("code");

                String expectedAuth = Base64.getEncoder()
                    .encodeToString("myId:".getBytes(StandardCharsets.UTF_8));

                wireMockServer.verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
                    .withHeader("Authorization", equalTo("Basic " + expectedAuth)));
            }
        }

        @Test
        @DisplayName("Custom apiBaseUrl is used for API calls")
        void customApiBaseUrl() {
            String baseUrl = "http://localhost:" + wireMockServer.port();
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);

            try (RedditClient customClient = RedditClient.builder()
                    .config(config)
                    .apiBaseUrl(baseUrl)
                    .tokenUrl(baseUrl + TOKEN_PATH)
                    .build()) {

                wireMockServer.stubFor(get(urlPathEqualTo(ME_PATH))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"user\",\"id\":\"t2_1\"}")));

                RedditUser user = customClient.getMe("token");

                assertEquals("user", user.name());
            }
        }

        @Test
        @DisplayName("Custom tokenUrl is used for token calls")
        void customTokenUrl() {
            String baseUrl = "http://localhost:" + wireMockServer.port();
            String customTokenPath = "/custom/token";
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);

            try (RedditClient customClient = RedditClient.builder()
                    .config(config)
                    .apiBaseUrl(baseUrl)
                    .tokenUrl(baseUrl + customTokenPath)
                    .build()) {

                wireMockServer.stubFor(post(urlPathEqualTo(customTokenPath))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(TOKEN_RESPONSE_JSON)));

                TokenResponse response = customClient.exchangeToken("code");

                assertEquals("at", response.accessToken());
                wireMockServer.verify(postRequestedFor(urlPathEqualTo(customTokenPath)));
            }
        }

        @Test
        @DisplayName("Custom authBaseUrl is used in authorization URL")
        void customAuthBaseUrl() {
            RedditConfig config = new RedditConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USER_AGENT);

            try (RedditClient customClient = RedditClient.builder()
                    .config(config)
                    .authBaseUrl("https://custom.auth.example.com/authorize")
                    .build()) {

                String url = customClient.getAuthorizationUrl("state", List.of("read"));

                assertTrue(url.startsWith("https://custom.auth.example.com/authorize?"));
            }
        }
    }

    // ========================================================================================
    // RedditConfig validation
    // ========================================================================================

    @Nested
    @DisplayName("RedditConfig validation")
    class RedditConfigValidation {

        @Test
        @DisplayName("Valid config - no exception")
        void validConfig() {
            assertDoesNotThrow(
                () -> new RedditConfig("clientId", "secret", "http://localhost/cb", "Agent/1.0"));
        }

        @Test
        @DisplayName("Null clientId throws IllegalArgumentException")
        void nullClientId() {
            assertThrows(IllegalArgumentException.class,
                () -> new RedditConfig(null, "secret", "http://localhost/cb", "Agent/1.0"));
        }

        @Test
        @DisplayName("Blank clientId throws IllegalArgumentException")
        void blankClientId() {
            assertThrows(IllegalArgumentException.class,
                () -> new RedditConfig("   ", "secret", "http://localhost/cb", "Agent/1.0"));
        }

        @Test
        @DisplayName("Null userAgent throws IllegalArgumentException")
        void nullUserAgent() {
            assertThrows(IllegalArgumentException.class,
                () -> new RedditConfig("clientId", "secret", "http://localhost/cb", null));
        }

        @Test
        @DisplayName("Blank userAgent throws IllegalArgumentException")
        void blankUserAgent() {
            assertThrows(IllegalArgumentException.class,
                () -> new RedditConfig("clientId", "secret", "http://localhost/cb", "  "));
        }

        @Test
        @DisplayName("Null clientSecret is allowed")
        void nullClientSecretAllowed() {
            assertDoesNotThrow(
                () -> new RedditConfig("clientId", null, "http://localhost/cb", "Agent/1.0"));
        }

        @Test
        @DisplayName("Null redirectUri is allowed")
        void nullRedirectUriAllowed() {
            assertDoesNotThrow(
                () -> new RedditConfig("clientId", "secret", null, "Agent/1.0"));
        }
    }
}
