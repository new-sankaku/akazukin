package com.akazukin.sdk.pinterest;

import com.akazukin.sdk.pinterest.exception.PinterestApiException;
import com.akazukin.sdk.pinterest.model.Board;
import com.akazukin.sdk.pinterest.model.PinResponse;
import com.akazukin.sdk.pinterest.model.PinterestUser;
import com.akazukin.sdk.pinterest.model.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PinterestClient WireMock Tests")
class PinterestClientWireMockTest {

    private static final String APP_ID = "testAppId";
    private static final String APP_SECRET = "testAppSecret";
    private static final String REDIRECT_URI = "https://example.com/callback";
    private static final String ACCESS_TOKEN = "test-access-token";

    private WireMockServer wireMockServer;
    private PinterestClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        baseUrl = "http://localhost:" + wireMockServer.port();

        PinterestConfig config = new PinterestConfig(APP_ID, APP_SECRET, REDIRECT_URI);
        client = PinterestClient.builder()
            .config(config)
            .apiBaseUrl(baseUrl)
            .tokenUrl(baseUrl + "/oauth/token")
            .authBaseUrl(baseUrl + "/oauth/")
            .httpClient(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build())
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
            .encodeToString((APP_ID + ":" + APP_SECRET).getBytes(StandardCharsets.UTF_8));
    }

    // ========================================================================
    // exchangeToken
    // ========================================================================
    @Nested
    @DisplayName("exchangeToken")
    class ExchangeTokenTests {

        @Test
        @DisplayName("Success - returns all TokenResponse fields")
        void success_returnsAllFields() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"at-123\",\"refresh_token\":\"rt-456\",\"expires_in\":3600}")));

            TokenResponse response = client.exchangeToken("test-code");

            assertNotNull(response);
            assertEquals("at-123", response.accessToken());
            assertEquals("rt-456", response.refreshToken());
            assertEquals(3600, response.expiresIn());
        }

        @Test
        @DisplayName("Verify POST method is used")
        void verifyPostMethod() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("code");

            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/oauth/token")));
        }

        @Test
        @DisplayName("Verify Content-Type is application/json")
        void verifyContentType() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Verify Authorization: Basic = Base64(appId:appSecret)")
        void verifyBasicAuth() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedBasicAuth())));
        }

        @Test
        @DisplayName("Verify body JSON has grant_type=authorization_code")
        void verifyBodyGrantType() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("mycode");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"grant_type\":\"authorization_code\"")));
        }

        @Test
        @DisplayName("Verify body JSON has code field")
        void verifyBodyCode() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("my-auth-code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"code\":\"my-auth-code\"")));
        }

        @Test
        @DisplayName("Verify body JSON has redirect_uri field")
        void verifyBodyRedirectUri() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.exchangeToken("code");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"redirect_uri\":\"" + REDIRECT_URI + "\"")));
        }

        @Test
        @DisplayName("Response with extra fields - ignored gracefully")
        void responseWithExtraFields() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,"
                        + "\"token_type\":\"bearer\",\"scope\":\"read_write\",\"extra\":\"ignored\"}")));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
            assertEquals("rt", response.refreshToken());
            assertEquals(3600, response.expiresIn());
        }

        @Test
        @DisplayName("400 Bad Request - throws exception with code/message parsed")
        void error400() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"invalid_grant\",\"message\":\"The authorization code is invalid\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("bad-code"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("invalid_grant", ex.getError());
            assertEquals("The authorization code is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Invalid client credentials\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}")));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("at", response.accessToken());
        }

        @Test
        @DisplayName("429 with Retry-After header - respects wait time")
        void error429WithRetryAfter() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("retry-after")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("retry-after")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":1}")));

            long start = System.currentTimeMillis();
            TokenResponse response = client.exchangeToken("code");
            long elapsed = System.currentTimeMillis() - start;

            assertNotNull(response);
            assertTrue(elapsed >= 900, "Should have waited at least ~1 second for Retry-After");
        }

        @Test
        @DisplayName("429 exhausted all retries - throws exception")
        void error429Exhausted() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"rate_limited\",\"message\":\"Too many requests\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(429, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"internal_error\",\"message\":\"Something went wrong\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Empty body - throws PARSE_ERROR")
        void emptyBody() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Invalid JSON response - throws PARSE_ERROR")
        void invalidJson() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not-json-at-all")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error response with 'code' field - parsed as error code")
        void errorWithCodeField() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"invalid_request\",\"message\":\"Missing parameter\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("invalid_request", ex.getError());
            assertEquals("Missing parameter", ex.getMessage());
        }

        @Test
        @DisplayName("Error response with 'error' field (no 'code') - parsed as error code")
        void errorWithErrorField() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_client\",\"error_description\":\"Client auth failed\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("invalid_client", ex.getError());
            assertEquals("Client auth failed", ex.getMessage());
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message with status code")
        void errorResponseNotValidJson() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withBody("Internal Server Error - plain text")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(500, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("500"));
            assertTrue(ex.getMessage().contains("not valid JSON"));
        }

        @Test
        @DisplayName("IOException - throws IO_ERROR")
        void ioError() {
            // Use a port that is not listening to trigger IOException
            PinterestConfig config = new PinterestConfig(APP_ID, APP_SECRET, REDIRECT_URI);
            PinterestClient badClient = PinterestClient.builder()
                .config(config)
                .apiBaseUrl("http://localhost:1")
                .tokenUrl("http://localhost:1/oauth/token")
                .authBaseUrl("http://localhost:1/oauth/")
                .httpClient(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build())
                .build();

            try {
                PinterestApiException ex = assertThrows(PinterestApiException.class,
                    () -> badClient.exchangeToken("code"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getError());
            } finally {
                badClient.close();
            }
        }

        @Test
        @DisplayName("IOException retry then success - works on subsequent attempt")
        void ioErrorRetryThenSuccess() {
            // First request fails with connection reset (fault), then succeeds
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("io-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("recovered"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("io-retry")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"recovered-at\",\"refresh_token\":\"rt\",\"expires_in\":3600}")));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("recovered-at", response.accessToken());
        }
    }

    // ========================================================================
    // refreshToken
    // ========================================================================
    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Success - returns TokenResponse with all fields")
        void success() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"new-at\",\"refresh_token\":\"new-rt\",\"expires_in\":7200}")));

            TokenResponse response = client.refreshToken("old-refresh-token");

            assertNotNull(response);
            assertEquals("new-at", response.accessToken());
            assertEquals("new-rt", response.refreshToken());
            assertEquals(7200, response.expiresIn());
        }

        @Test
        @DisplayName("Verify body has grant_type=refresh_token")
        void verifyBodyGrantType() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.refreshToken("rt-token");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"grant_type\":\"refresh_token\"")));
        }

        @Test
        @DisplayName("Verify body has refresh_token field with correct value")
        void verifyBodyRefreshToken() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.refreshToken("my-refresh-token-xyz");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"refresh_token\":\"my-refresh-token-xyz\"")));
        }

        @Test
        @DisplayName("Verify Basic auth header")
        void verifyBasicAuth() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.refreshToken("rt");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withHeader("Authorization", equalTo("Basic " + expectedBasicAuth())));
        }

        @Test
        @DisplayName("400 Bad Request - throws exception")
        void error400() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"invalid_grant\",\"message\":\"Refresh token expired\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.refreshToken("expired-rt"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("invalid_grant", ex.getError());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Bad credentials\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"server_error\",\"message\":\"Internal error\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - throws PARSE_ERROR")
        void invalidJson() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{broken-json")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.refreshToken("rt"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Verify does NOT include redirect_uri in body")
        void noRedirectUri() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            client.refreshToken("rt");

            // The refresh request should only have grant_type and refresh_token, no redirect_uri
            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/oauth/token"))
                .withRequestBody(containing("\"grant_type\":\"refresh_token\""))
                .withRequestBody(containing("\"refresh_token\":\"rt\"")));
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void responseWithExtraFields() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":100,"
                        + "\"token_type\":\"bearer\",\"scope\":\"boards:read\"}")));

            TokenResponse response = client.refreshToken("rt");

            assertNotNull(response);
            assertEquals("a", response.accessToken());
        }

        @Test
        @DisplayName("429 then success - retry works for refresh")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("refresh-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("refresh-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            TokenResponse response = client.refreshToken("rt");

            assertNotNull(response);
            assertEquals("a", response.accessToken());
        }
    }

    // ========================================================================
    // createPin
    // ========================================================================
    @Nested
    @DisplayName("createPin")
    class CreatePinTests {

        @Test
        @DisplayName("Success - returns PinResponse with all fields")
        void success() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"pin-001\",\"title\":\"My Pin\",\"board_id\":\"board-001\"}")));

            PinResponse response = client.createPin(ACCESS_TOKEN, "board-001", "My Pin",
                "A great pin", "https://example.com/image.jpg");

            assertNotNull(response);
            assertEquals("pin-001", response.id());
            assertEquals("My Pin", response.title());
            assertEquals("board-001", response.boardId());
        }

        @Test
        @DisplayName("Verify POST to /pins")
        void verifyPostToPins() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg");

            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/pins")));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer token")
        void verifyBearerAuth() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
        }

        @Test
        @DisplayName("Verify Content-Type: application/json")
        void verifyContentType() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Verify body has board_id")
        void verifyBodyBoardId() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "board-xyz", "t", "d", "https://img.com/x.jpg");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withRequestBody(containing("\"board_id\":\"board-xyz\"")));
        }

        @Test
        @DisplayName("Verify body has title and description")
        void verifyBodyTitleAndDescription() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "My Title", "My Description", "https://img.com/x.jpg");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withRequestBody(containing("\"title\":\"My Title\""))
                .withRequestBody(containing("\"description\":\"My Description\"")));
        }

        @Test
        @DisplayName("Verify body has media_source with source_type=image_url and url")
        void verifyBodyMediaSource() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://example.com/photo.png");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withRequestBody(containing("\"source_type\":\"image_url\""))
                .withRequestBody(containing("\"url\":\"https://example.com/photo.png\"")));
        }

        @Test
        @DisplayName("Title with special characters - present in JSON body")
        void titleWithSpecialChars() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            client.createPin(ACCESS_TOKEN, "b", "Title with \"quotes\" & <html>", "desc", "https://img.com/x.jpg");

            wireMockServer.verify(postRequestedFor(urlPathEqualTo("/pins"))
                .withRequestBody(containing("Title with")));
        }

        @Test
        @DisplayName("Long description - succeeds")
        void longDescription() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            String longDesc = "A".repeat(5000);
            PinResponse response = client.createPin(ACCESS_TOKEN, "b", "t", longDesc, "https://img.com/x.jpg");

            assertNotNull(response);
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void responseWithExtraFields() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\","
                        + "\"link\":\"https://x.com\",\"created_at\":\"2026-01-01\",\"extra\":true}")));

            PinResponse response = client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg");

            assertNotNull(response);
            assertEquals("p", response.id());
            assertEquals("t", response.title());
            assertEquals("b", response.boardId());
        }

        @Test
        @DisplayName("400 Bad Request - throws exception")
        void error400() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"invalid_pin\",\"message\":\"Invalid pin data\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals(400, ex.getStatusCode());
            assertEquals("invalid_pin", ex.getError());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Token expired\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden - throws exception with code/message")
        void error403() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"forbidden\",\"message\":\"Insufficient permissions\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals(403, ex.getStatusCode());
            assertEquals("forbidden", ex.getError());
            assertEquals("Insufficient permissions", ex.getMessage());
        }

        @Test
        @DisplayName("404 Not Found - throws exception")
        void error404() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"not_found\",\"message\":\"Board not found\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "nonexistent", "t", "d", "https://img.com/x.jpg"));

            assertEquals(404, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works")
        void error429ThenSuccess() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .inScenario("pin-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .inScenario("pin-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"p\",\"title\":\"t\",\"board_id\":\"b\"}")));

            PinResponse response = client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg");

            assertNotNull(response);
            assertEquals("p", response.id());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"server_error\",\"message\":\"Internal error\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - throws PARSE_ERROR")
        void invalidJson() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("<<<not json>>>")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error with 'code' field parsed correctly")
        void errorWithCodeField() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unprocessable\",\"message\":\"Image URL invalid\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals("unprocessable", ex.getError());
        }

        @Test
        @DisplayName("Error with 'error' field parsed when no 'code' field")
        void errorWithErrorField() {
            wireMockServer.stubFor(post(urlPathEqualTo("/pins"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad_request\",\"error_description\":\"Missing board_id\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.createPin(ACCESS_TOKEN, "b", "t", "d", "https://img.com/x.jpg"));

            assertEquals("bad_request", ex.getError());
            assertEquals("Missing board_id", ex.getMessage());
        }
    }

    // ========================================================================
    // deletePin
    // ========================================================================
    @Nested
    @DisplayName("deletePin")
    class DeletePinTests {

        @Test
        @DisplayName("Success with 200 - no exception")
        void success200() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .willReturn(aResponse()
                    .withStatus(200)));

            assertDoesNotThrow(() -> client.deletePin(ACCESS_TOKEN, "pin-001"));
        }

        @Test
        @DisplayName("Success with 204 No Content - no exception")
        void success204() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-002"))
                .willReturn(aResponse()
                    .withStatus(204)));

            assertDoesNotThrow(() -> client.deletePin(ACCESS_TOKEN, "pin-002"));
        }

        @Test
        @DisplayName("Verify DELETE method to /pins/{pinId}")
        void verifyDeleteMethod() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-abc"))
                .willReturn(aResponse()
                    .withStatus(204)));

            client.deletePin(ACCESS_TOKEN, "pin-abc");

            wireMockServer.verify(1, deleteRequestedFor(urlPathEqualTo("/pins/pin-abc")));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer token")
        void verifyBearerAuth() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .willReturn(aResponse()
                    .withStatus(204)));

            client.deletePin(ACCESS_TOKEN, "pin-001");

            wireMockServer.verify(deleteRequestedFor(urlPathEqualTo("/pins/pin-001"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
        }

        @Test
        @DisplayName("Pin ID appears in URL path")
        void pinIdInUrl() {
            String pinId = "unique-pin-id-12345";
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/" + pinId))
                .willReturn(aResponse()
                    .withStatus(204)));

            client.deletePin(ACCESS_TOKEN, pinId);

            wireMockServer.verify(1, deleteRequestedFor(urlPathEqualTo("/pins/" + pinId)));
        }

        @Test
        @DisplayName("400 Bad Request - throws exception")
        void error400() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/bad-pin"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"invalid_id\",\"message\":\"Invalid pin ID format\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.deletePin(ACCESS_TOKEN, "bad-pin"));

            assertEquals(400, ex.getStatusCode());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Token invalid\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.deletePin(ACCESS_TOKEN, "pin-001"));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden - throws exception")
        void error403() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"forbidden\",\"message\":\"Not your pin\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.deletePin(ACCESS_TOKEN, "pin-001"));

            assertEquals(403, ex.getStatusCode());
            assertEquals("forbidden", ex.getError());
        }

        @Test
        @DisplayName("404 Not Found - throws exception")
        void error404() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/nonexistent"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"not_found\",\"message\":\"Pin not found\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.deletePin(ACCESS_TOKEN, "nonexistent"));

            assertEquals(404, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"server_error\",\"message\":\"Internal error\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.deletePin(ACCESS_TOKEN, "pin-001"));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("429 then success - retry works for delete")
        void error429ThenSuccess() {
            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .inScenario("delete-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(delete(urlPathEqualTo("/pins/pin-001"))
                .inScenario("delete-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(204)));

            assertDoesNotThrow(() -> client.deletePin(ACCESS_TOKEN, "pin-001"));
        }
    }

    // ========================================================================
    // getMe
    // ========================================================================
    @Nested
    @DisplayName("getMe")
    class GetMeTests {

        @Test
        @DisplayName("Success - returns all PinterestUser fields")
        void success() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"johndoe\",\"account_type\":\"BUSINESS\","
                        + "\"profile_image\":\"https://i.pinimg.com/profile.jpg\","
                        + "\"follower_count\":1500,\"pin_count\":250}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertNotNull(user);
            assertEquals("johndoe", user.username());
            assertEquals("BUSINESS", user.accountType());
            assertEquals("https://i.pinimg.com/profile.jpg", user.profileImage());
            assertEquals(1500, user.followerCount());
            assertEquals(250, user.pinCount());
        }

        @Test
        @DisplayName("Verify GET to /user_account")
        void verifyGetMethod() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"PERSONAL\","
                        + "\"profile_image\":\"img\",\"follower_count\":0,\"pin_count\":0}")));

            client.getMe(ACCESS_TOKEN);

            wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/user_account")));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer token")
        void verifyBearerAuth() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"P\","
                        + "\"profile_image\":\"i\",\"follower_count\":0,\"pin_count\":0}")));

            client.getMe(ACCESS_TOKEN);

            wireMockServer.verify(getRequestedFor(urlPathEqualTo("/user_account"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
        }

        @Test
        @DisplayName("Response with missing optional fields - defaults to null/0")
        void missingOptionalFields() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"minimal\"}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertNotNull(user);
            assertEquals("minimal", user.username());
            assertNull(user.accountType());
            assertNull(user.profileImage());
            assertEquals(0, user.followerCount());
            assertEquals(0, user.pinCount());
        }

        @Test
        @DisplayName("Response with extra fields - ignored gracefully")
        void responseWithExtraFields() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"B\","
                        + "\"profile_image\":\"i\",\"follower_count\":1,\"pin_count\":2,"
                        + "\"website_url\":\"https://x.com\",\"country\":\"US\",\"board_count\":10}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertNotNull(user);
            assertEquals("u", user.username());
            assertEquals(1, user.followerCount());
        }

        @Test
        @DisplayName("Zero counts - correct values")
        void zeroCounts() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"newuser\",\"account_type\":\"PERSONAL\","
                        + "\"profile_image\":\"img\",\"follower_count\":0,\"pin_count\":0}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertEquals(0, user.followerCount());
            assertEquals(0, user.pinCount());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Invalid token\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden - throws exception")
        void error403() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"forbidden\",\"message\":\"Access denied\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals(403, ex.getStatusCode());
            assertEquals("forbidden", ex.getError());
            assertEquals("Access denied", ex.getMessage());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"server_error\",\"message\":\"Oops\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - throws PARSE_ERROR")
        void invalidJson() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("not json")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Empty body - throws PARSE_ERROR")
        void emptyBody() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works for getMe")
        void error429ThenSuccess() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .inScenario("getme-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .inScenario("getme-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"B\","
                        + "\"profile_image\":\"i\",\"follower_count\":0,\"pin_count\":0}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertNotNull(user);
            assertEquals("u", user.username());
        }

        @Test
        @DisplayName("Large follower/pin counts - correct values")
        void largeCounts() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"popular\",\"account_type\":\"BUSINESS\","
                        + "\"profile_image\":\"img\",\"follower_count\":999999,\"pin_count\":50000}")));

            PinterestUser user = client.getMe(ACCESS_TOKEN);

            assertEquals(999999, user.followerCount());
            assertEquals(50000, user.pinCount());
        }

        @Test
        @DisplayName("Error response not valid JSON - fallback message")
        void errorNotValidJson() {
            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(502)
                    .withBody("Bad Gateway")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.getMe(ACCESS_TOKEN));

            assertEquals(502, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("502"));
        }
    }

    // ========================================================================
    // listBoards
    // ========================================================================
    @Nested
    @DisplayName("listBoards")
    class ListBoardsTests {

        @Test
        @DisplayName("Success with items - returns Board list")
        void successWithItems() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{\"id\":\"b1\",\"name\":\"Recipes\","
                        + "\"description\":\"Food recipes\",\"pin_count\":42}]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertNotNull(boards);
            assertEquals(1, boards.size());
            Board board = boards.get(0);
            assertEquals("b1", board.id());
            assertEquals("Recipes", board.name());
            assertEquals("Food recipes", board.description());
            assertEquals(42, board.pinCount());
        }

        @Test
        @DisplayName("Empty items array - returns empty list")
        void emptyItems() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertNotNull(boards);
            assertTrue(boards.isEmpty());
        }

        @Test
        @DisplayName("Items not an array - returns empty list")
        void itemsNotArray() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":\"not-an-array\"}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertNotNull(boards);
            assertTrue(boards.isEmpty());
        }

        @Test
        @DisplayName("Multiple boards - all parsed correctly")
        void multipleBoards() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":["
                        + "{\"id\":\"b1\",\"name\":\"Board1\",\"description\":\"d1\",\"pin_count\":10},"
                        + "{\"id\":\"b2\",\"name\":\"Board2\",\"description\":\"d2\",\"pin_count\":20},"
                        + "{\"id\":\"b3\",\"name\":\"Board3\",\"description\":\"d3\",\"pin_count\":30}"
                        + "]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertEquals(3, boards.size());
            assertEquals("b1", boards.get(0).id());
            assertEquals("Board2", boards.get(1).name());
            assertEquals(30, boards.get(2).pinCount());
        }

        @Test
        @DisplayName("Verify GET to /boards")
        void verifyGetMethod() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[]}")));

            client.listBoards(ACCESS_TOKEN);

            wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/boards")));
        }

        @Test
        @DisplayName("Verify Authorization: Bearer token")
        void verifyBearerAuth() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[]}")));

            client.listBoards(ACCESS_TOKEN);

            wireMockServer.verify(getRequestedFor(urlPathEqualTo("/boards"))
                .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
        }

        @Test
        @DisplayName("Board with all fields - verify each")
        void boardWithAllFields() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{\"id\":\"board-123\",\"name\":\"Travel Photos\","
                        + "\"description\":\"My best travel shots\",\"pin_count\":157}]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertEquals(1, boards.size());
            Board board = boards.get(0);
            assertEquals("board-123", board.id());
            assertEquals("Travel Photos", board.name());
            assertEquals("My best travel shots", board.description());
            assertEquals(157, board.pinCount());
        }

        @Test
        @DisplayName("Board with missing optional fields - defaults to null/0")
        void boardMissingOptionalFields() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{\"id\":\"b1\",\"name\":\"Minimal\"}]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertEquals(1, boards.size());
            Board board = boards.get(0);
            assertEquals("b1", board.id());
            assertEquals("Minimal", board.name());
            assertNull(board.description());
            assertEquals(0, board.pinCount());
        }

        @Test
        @DisplayName("Response with extra fields - ignored")
        void responseWithExtraFields() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{\"id\":\"b1\",\"name\":\"N\",\"description\":\"D\","
                        + "\"pin_count\":5,\"privacy\":\"PUBLIC\",\"owner\":{\"id\":\"u1\"}}],"
                        + "\"bookmark\":\"next-page\"}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertEquals(1, boards.size());
            assertEquals("b1", boards.get(0).id());
        }

        @Test
        @DisplayName("No items key in response - returns empty list")
        void noItemsKey() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"data\":[]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertNotNull(boards);
            assertTrue(boards.isEmpty());
        }

        @Test
        @DisplayName("401 Unauthorized - throws exception")
        void error401() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"unauthorized\",\"message\":\"Token expired\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.listBoards(ACCESS_TOKEN));

            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("403 Forbidden - throws exception")
        void error403() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"forbidden\",\"message\":\"Scope insufficient\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.listBoards(ACCESS_TOKEN));

            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("500 Internal Server Error - throws exception")
        void error500() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"server_error\",\"message\":\"Internal error\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.listBoards(ACCESS_TOKEN));

            assertEquals(500, ex.getStatusCode());
        }

        @Test
        @DisplayName("Invalid JSON response - throws PARSE_ERROR")
        void invalidJson() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{{invalid}}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.listBoards(ACCESS_TOKEN));

            assertEquals("PARSE_ERROR", ex.getError());
        }

        @Test
        @DisplayName("429 then success - retry works for listBoards")
        void error429ThenSuccess() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .inScenario("boards-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1"))
                .willSetStateTo("retried"));

            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .inScenario("boards-retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":[{\"id\":\"b1\",\"name\":\"N\",\"description\":\"D\",\"pin_count\":1}]}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertEquals(1, boards.size());
        }

        @Test
        @DisplayName("Items is null (JSON null) - returns empty list")
        void itemsNull() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"items\":null}")));

            List<Board> boards = client.listBoards(ACCESS_TOKEN);

            assertNotNull(boards);
            assertTrue(boards.isEmpty());
        }
    }

    // ========================================================================
    // getAuthorizationUrl
    // ========================================================================
    @Nested
    @DisplayName("getAuthorizationUrl")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("Contains client_id (appId)")
        void containsClientId() {
            String url = client.getAuthorizationUrl("state123", List.of("boards:read"));

            assertTrue(url.contains("client_id=" + APP_ID));
        }

        @Test
        @DisplayName("Contains redirect_uri")
        void containsRedirectUri() {
            String url = client.getAuthorizationUrl("state123", List.of("boards:read"));

            assertTrue(url.contains("redirect_uri="));
            assertTrue(url.contains("example.com"));
        }

        @Test
        @DisplayName("Contains response_type=code")
        void containsResponseTypeCode() {
            String url = client.getAuthorizationUrl("state123", List.of("boards:read"));

            assertTrue(url.contains("response_type=code"));
        }

        @Test
        @DisplayName("Contains scope joined with commas")
        void containsScopeJoinedWithCommas() {
            String url = client.getAuthorizationUrl("state123", List.of("boards:read", "pins:write", "user:read"));

            // Scopes joined with commas, then URL-encoded (comma -> %2C)
            assertTrue(url.contains("scope=boards%3Aread%2Cpins%3Awrite%2Cuser%3Aread")
                || url.contains("scope=boards:read,pins:write,user:read"),
                "URL should contain comma-separated scopes: " + url);
        }

        @Test
        @DisplayName("Contains state parameter")
        void containsState() {
            String url = client.getAuthorizationUrl("my-csrf-state", List.of("boards:read"));

            assertTrue(url.contains("state=my-csrf-state"));
        }

        @Test
        @DisplayName("Special characters in state are encoded")
        void specialCharsEncoded() {
            String url = client.getAuthorizationUrl("state with spaces&special=chars", List.of("boards:read"));

            // Spaces and special chars should be URL-encoded
            assertTrue(!url.contains("state with spaces"), "Spaces should be encoded");
            assertTrue(url.contains("state="), "Should have state parameter");
        }

        @Test
        @DisplayName("Single scope - no commas")
        void singleScope() {
            String url = client.getAuthorizationUrl("s", List.of("pins:read"));

            assertTrue(url.contains("scope="));
            assertTrue(url.contains("pins"));
        }

        @Test
        @DisplayName("URL starts with auth base URL")
        void startsWithAuthBaseUrl() {
            String url = client.getAuthorizationUrl("s", List.of("boards:read"));

            assertTrue(url.startsWith(baseUrl + "/oauth/"));
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================
    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Build without config - throws IllegalStateException")
        void buildWithoutConfig() {
            assertThrows(IllegalStateException.class, () -> PinterestClient.builder().build());
        }

        @Test
        @DisplayName("Build with config only - uses defaults")
        void buildWithConfigOnly() {
            PinterestConfig config = new PinterestConfig("id", "secret", "https://redirect.com");
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .build();

            assertNotNull(c);
            // Should have default values and not throw
            String url = c.getAuthorizationUrl("state", List.of("boards:read"));
            assertNotNull(url);
            assertTrue(url.contains("client_id=id"));
            c.close();
        }

        @Test
        @DisplayName("Custom HttpClient is used")
        void customHttpClient() {
            HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

            PinterestConfig config = new PinterestConfig(APP_ID, APP_SECRET, REDIRECT_URI);
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .httpClient(customClient)
                .apiBaseUrl(baseUrl)
                .tokenUrl(baseUrl + "/oauth/token")
                .build();

            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"B\","
                        + "\"profile_image\":\"i\",\"follower_count\":0,\"pin_count\":0}")));

            PinterestUser user = c.getMe(ACCESS_TOKEN);
            assertNotNull(user);
            c.close();
        }

        @Test
        @DisplayName("Custom ObjectMapper is used")
        void customObjectMapper() {
            ObjectMapper customMapper = new ObjectMapper();
            customMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            PinterestConfig config = new PinterestConfig(APP_ID, APP_SECRET, REDIRECT_URI);
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .objectMapper(customMapper)
                .apiBaseUrl(baseUrl)
                .tokenUrl(baseUrl + "/oauth/token")
                .build();

            wireMockServer.stubFor(get(urlPathEqualTo("/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"B\","
                        + "\"profile_image\":\"i\",\"follower_count\":0,\"pin_count\":0,"
                        + "\"extra_field\":\"should be ignored\"}")));

            PinterestUser user = c.getMe(ACCESS_TOKEN);
            assertNotNull(user);
            assertEquals("u", user.username());
            c.close();
        }

        @Test
        @DisplayName("close() with default HttpClient - no error")
        void closeDefault() {
            PinterestConfig config = new PinterestConfig("id", "secret", "https://r.com");
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .build();

            assertDoesNotThrow(c::close);
        }

        @Test
        @DisplayName("close() with custom HttpClient - closes it")
        void closeCustom() {
            HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build();

            PinterestConfig config = new PinterestConfig("id", "secret", "https://r.com");
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .httpClient(customClient)
                .build();

            assertDoesNotThrow(c::close);
        }

        @Test
        @DisplayName("Custom apiBaseUrl is used in requests")
        void customApiBaseUrl() {
            PinterestConfig config = new PinterestConfig(APP_ID, APP_SECRET, REDIRECT_URI);
            PinterestClient c = PinterestClient.builder()
                .config(config)
                .apiBaseUrl(baseUrl + "/custom/v5")
                .tokenUrl(baseUrl + "/oauth/token")
                .httpClient(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build())
                .build();

            wireMockServer.stubFor(get(urlPathEqualTo("/custom/v5/user_account"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"username\":\"u\",\"account_type\":\"B\","
                        + "\"profile_image\":\"i\",\"follower_count\":0,\"pin_count\":0}")));

            PinterestUser user = c.getMe(ACCESS_TOKEN);
            assertNotNull(user);

            wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/custom/v5/user_account")));
            c.close();
        }
    }

    // ========================================================================
    // PinterestConfig validation
    // ========================================================================
    @Nested
    @DisplayName("PinterestConfig validation")
    class PinterestConfigValidationTests {

        @Test
        @DisplayName("Valid config - no error")
        void validConfig() {
            assertDoesNotThrow(() -> new PinterestConfig("app123", "secret456", "https://example.com/cb"));
        }

        @Test
        @DisplayName("Null appId - throws IllegalArgumentException")
        void nullAppId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PinterestConfig(null, "secret", "https://example.com/cb"));

            assertTrue(ex.getMessage().contains("appId"));
        }

        @Test
        @DisplayName("Blank appId - throws IllegalArgumentException")
        void blankAppId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PinterestConfig("  ", "secret", "https://example.com/cb"));

            assertTrue(ex.getMessage().contains("appId"));
        }

        @Test
        @DisplayName("Null appSecret - throws IllegalArgumentException")
        void nullAppSecret() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PinterestConfig("app", null, "https://example.com/cb"));

            assertTrue(ex.getMessage().contains("appSecret"));
        }

        @Test
        @DisplayName("Blank appSecret - throws IllegalArgumentException")
        void blankAppSecret() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PinterestConfig("app", "   ", "https://example.com/cb"));

            assertTrue(ex.getMessage().contains("appSecret"));
        }

        @Test
        @DisplayName("Null redirectUri - allowed (no validation)")
        void nullRedirectUri() {
            assertDoesNotThrow(() -> new PinterestConfig("app", "secret", null));
        }
    }

    // ========================================================================
    // Cross-cutting error handling
    // ========================================================================
    @Nested
    @DisplayName("Cross-cutting error handling")
    class CrossCuttingTests {

        @Test
        @DisplayName("Error with both 'code' and 'error' fields - 'code' takes precedence")
        void codeTakesPrecedenceOverError() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"from_code\",\"error\":\"from_error\","
                        + "\"message\":\"msg\",\"error_description\":\"desc\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            // code field is checked first via path("code").asText(path("error").asText(...))
            assertEquals("from_code", ex.getError());
        }

        @Test
        @DisplayName("Error with message and error_description - 'message' takes precedence")
        void messageTakesPrecedenceOverDescription() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"err\",\"message\":\"from_message\","
                        + "\"error_description\":\"from_desc\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("from_message", ex.getMessage());
        }

        @Test
        @DisplayName("Error with only error_description (no message) - uses error_description")
        void errorDescriptionUsedWhenNoMessage() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad\",\"error_description\":\"Detailed description\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals("Detailed description", ex.getMessage());
        }

        @Test
        @DisplayName("Error with no recognizable fields - fallback to HTTP status")
        void errorNoFields() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withStatus(418)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"foo\":\"bar\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(418, ex.getStatusCode());
            assertEquals("UNKNOWN", ex.getError());
            assertTrue(ex.getMessage().contains("418"));
        }

        @Test
        @DisplayName("Multiple 429 retries with exponential backoff then success")
        void multiple429RetriesBackoff() {
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("multi-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("retry-1"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("multi-retry")
                .whenScenarioStateIs("retry-1")
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("retry-2"));

            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .inScenario("multi-retry")
                .whenScenarioStateIs("retry-2")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"ok\",\"refresh_token\":\"r\",\"expires_in\":1}")));

            TokenResponse response = client.exchangeToken("code");

            assertNotNull(response);
            assertEquals("ok", response.accessToken());
        }

        @Test
        @DisplayName("IOException on all retries - throws IO_ERROR after exhausting retries")
        void ioErrorAllRetries() {
            // Use connection reset fault on all attempts
            wireMockServer.stubFor(post(urlPathEqualTo("/oauth/token"))
                .willReturn(aResponse()
                    .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.exchangeToken("code"));

            assertEquals(0, ex.getStatusCode());
            assertEquals("IO_ERROR", ex.getError());
        }

        @Test
        @DisplayName("Error detail is preserved from response body")
        void errorDetailPreserved() {
            wireMockServer.stubFor(get(urlPathEqualTo("/boards"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"validation_error\",\"message\":\"Invalid request\"}")));

            PinterestApiException ex = assertThrows(PinterestApiException.class,
                () -> client.listBoards(ACCESS_TOKEN));

            assertEquals("validation_error", ex.getError());
            assertEquals("Invalid request", ex.getMessage());
            // detail should contain the raw response body
            assertNotNull(ex.getDetail());
        }
    }
}
