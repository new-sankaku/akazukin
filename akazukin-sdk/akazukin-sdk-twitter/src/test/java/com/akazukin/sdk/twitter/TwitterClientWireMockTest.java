package com.akazukin.sdk.twitter;

import com.akazukin.sdk.twitter.auth.OAuth2PkceFlow;
import com.akazukin.sdk.twitter.exception.TwitterApiException;
import com.akazukin.sdk.twitter.model.TokenResponse;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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

class TwitterClientWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private TwitterClient client;
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String REDIRECT_URI = "http://localhost/callback";

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
        client = TwitterClient.builder()
            .config(config)
            .apiBaseUrl(baseUrl)
            .tokenUrl(baseUrl + "/oauth2/token")
            .authBaseUrl(baseUrl + "/authorize")
            .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    // ========================================================================
    // postTweet
    // ========================================================================
    @Nested
    @DisplayName("postTweet")
    class PostTweetTests {

        @Nested
        @DisplayName("Normal cases")
        class NormalCases {

            @Test
            @DisplayName("Normal text returns success with correct TweetResponse fields")
            void normalText_success() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"12345\",\"text\":\"hello world\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "hello world");

                assertNotNull(response);
                assertEquals("12345", response.id());
                assertEquals("hello world", response.text());
            }

            @Test
            @DisplayName("Max length text (280 chars) returns success")
            void maxLengthText_success() {
                String text280 = "a".repeat(280);
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"" + text280 + "\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, text280);

                assertEquals(text280, response.text());
            }

            @Test
            @DisplayName("Text with emoji returns success")
            void textWithEmoji_success() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"Hello \\ud83d\\ude00\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "Hello \ud83d\ude00");

                assertNotNull(response);
                assertEquals("1", response.id());
            }

            @Test
            @DisplayName("Text with Japanese/multibyte chars returns success")
            void textWithJapanese_success() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"\\u3053\\u3093\\u306b\\u3061\\u306f\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "\u3053\u3093\u306b\u3061\u306f");

                assertNotNull(response);
            }

            @Test
            @DisplayName("Text with URLs returns success")
            void textWithUrls_success() {
                String text = "Check out https://example.com/path?q=1&r=2";
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"" + text + "\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, text);

                assertNotNull(response);
            }

            @Test
            @DisplayName("Response with unknown extra fields is ignored and still succeeds")
            void responseWithExtraFields_ignored() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"hi\",\"extra_field\":\"ignored\"},\"meta\":{\"sent\":\"2026-01-01\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "hi");

                assertEquals("1", response.id());
                assertEquals("hi", response.text());
            }
        }

        @Nested
        @DisplayName("Request verification")
        class RequestVerification {

            @Test
            @DisplayName("Request has Authorization: Bearer header")
            void verifyAuthorizationHeader() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"hi\"}}")));

                client.postTweet(ACCESS_TOKEN, "hi");

                wireMock.verify(postRequestedFor(urlEqualTo("/tweets"))
                    .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
            }

            @Test
            @DisplayName("Request has Content-Type: application/json")
            void verifyContentType() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"hi\"}}")));

                client.postTweet(ACCESS_TOKEN, "hi");

                wireMock.verify(postRequestedFor(urlEqualTo("/tweets"))
                    .withHeader("Content-Type", equalTo("application/json")));
            }

            @Test
            @DisplayName("Request body contains {\"text\": \"...\"}")
            void verifyRequestBody() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"test msg\"}}")));

                client.postTweet(ACCESS_TOKEN, "test msg");

                wireMock.verify(postRequestedFor(urlEqualTo("/tweets"))
                    .withRequestBody(equalToJson("{\"text\":\"test msg\"}")));
            }

            @Test
            @DisplayName("Request URL is /tweets")
            void verifyRequestUrl() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"hi\"}}")));

                client.postTweet(ACCESS_TOKEN, "hi");

                wireMock.verify(postRequestedFor(urlEqualTo("/tweets")));
            }
        }

        @Nested
        @DisplayName("HTTP error cases")
        class HttpErrorCases {

            @Test
            @DisplayName("400 Bad Request with errors array throws TwitterApiException")
            void badRequest_withErrors() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errors":[{"message":"Invalid request","type":"about:blank","detail":"One or more parameters is invalid."}]}
                            """)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(400, ex.getStatusCode());
                assertEquals("Invalid request", ex.getMessage());
                assertEquals("about:blank", ex.getErrorCode());
                assertEquals("One or more parameters is invalid.", ex.getDetail());
            }

            @Test
            @DisplayName("401 Unauthorized throws TwitterApiException")
            void unauthorized() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errors":[{"message":"Unauthorized","type":"https://api.twitter.com/2/problems/not-authorized-for-resource","detail":"Unauthorized"}]}
                            """)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(401, ex.getStatusCode());
            }

            @Test
            @DisplayName("403 Forbidden with errors array verifies errorCode from type, message, detail")
            void forbidden_withErrors() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errors":[{"message":"Forbidden","type":"https://api.twitter.com/2/problems/forbidden","detail":"You are not permitted."}]}
                            """)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(403, ex.getStatusCode());
                assertEquals("https://api.twitter.com/2/problems/forbidden", ex.getErrorCode());
                assertEquals("Forbidden", ex.getMessage());
                assertEquals("You are not permitted.", ex.getDetail());
            }

            @Test
            @DisplayName("404 Not Found throws TwitterApiException")
            void notFound() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errors":[{"message":"Not Found","type":"about:blank","detail":"Resource not found"}]}
                            """)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(404, ex.getStatusCode());
            }

            @Test
            @DisplayName("429 Rate Limit then success on retry returns result using Scenario")
            void rateLimitThenSuccess() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("rateLimitRetry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "1")
                        .withBody("{\"errors\":[{\"message\":\"Too Many Requests\"}]}"))
                    .willSetStateTo("RETRY_1"));

                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("rateLimitRetry")
                    .whenScenarioStateIs("RETRY_1")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"999\",\"text\":\"retried\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "retried");

                assertNotNull(response);
                assertEquals("999", response.id());
            }

            @Test
            @DisplayName("429 with Retry-After header respects wait time")
            void rateLimitWithRetryAfter() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("retryAfter")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1")
                        .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                    .willSetStateTo("SUCCESS"));

                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("retryAfter")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"ok\"}}")));

                long start = System.currentTimeMillis();
                TweetResponse response = client.postTweet(ACCESS_TOKEN, "ok");
                long elapsed = System.currentTimeMillis() - start;

                assertNotNull(response);
                assertTrue(elapsed >= 900, "Should have waited at least ~1 second for Retry-After");
            }

            @Test
            @DisplayName("429 all retries exhausted throws TwitterApiException")
            void rateLimitAllRetriesExhausted() {
                // All 4 attempts (0,1,2,3) return 429 — the last one (attempt == maxRetries) returns the 429 response
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Too Many Requests\",\"type\":\"rate_limit\",\"detail\":\"Rate limit exceeded\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(429, ex.getStatusCode());
            }

            @Test
            @DisplayName("500 Server Error throws TwitterApiException")
            void serverError() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Internal Server Error\",\"type\":\"server_error\",\"detail\":\"Something went wrong\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(500, ex.getStatusCode());
            }

            @Test
            @DisplayName("502 Bad Gateway throws TwitterApiException")
            void badGateway() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(502)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Bad Gateway\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(502, ex.getStatusCode());
            }

            @Test
            @DisplayName("503 Service Unavailable throws TwitterApiException")
            void serviceUnavailable() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Service Unavailable\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(503, ex.getStatusCode());
            }
        }

        @Nested
        @DisplayName("Response body error cases")
        class ResponseBodyErrors {

            @Test
            @DisplayName("Response missing 'data' field throws TwitterApiException")
            void missingDataField() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"meta\":{\"sent\":true}}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(200, ex.getStatusCode());
                assertEquals("UNKNOWN", ex.getErrorCode());
                assertTrue(ex.getMessage().contains("missing 'data' field"));
            }

            @Test
            @DisplayName("Response with data: null returns null (NullNode passes null check, treeToValue returns null)")
            void dataFieldNull() {
                // When JSON has "data": null, Jackson's root.get("data") returns a NullNode (not Java null).
                // The code's null check (data == null) does NOT catch NullNode.
                // treeToValue(NullNode, TweetResponse.class) returns null.
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":null}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "hi");

                // NullNode deserialized as null by Jackson's treeToValue
                assertNull(response);
            }

            @Test
            @DisplayName("Response body is empty string throws UNKNOWN exception (missing data field)")
            void emptyResponseBody() {
                // Jackson's readTree("") returns a MissingNode or null-like node where get("data") returns null.
                // This triggers the "missing 'data' field" exception with errorCode UNKNOWN.
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals("UNKNOWN", ex.getErrorCode());
                assertTrue(ex.getMessage().contains("missing 'data' field"));
            }

            @Test
            @DisplayName("Response body is invalid JSON throws PARSE_ERROR exception")
            void invalidJsonBody() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not json at all {{")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals("PARSE_ERROR", ex.getErrorCode());
            }

            @Test
            @DisplayName("Error response with multiple 'errors' array elements uses first error")
            void multipleErrorsUsesFirst() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"errors":[
                              {"message":"First error","type":"first_type","detail":"First detail"},
                              {"message":"Second error","type":"second_type","detail":"Second detail"}
                            ]}
                            """)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals("First error", ex.getMessage());
                assertEquals("first_type", ex.getErrorCode());
                assertEquals("First detail", ex.getDetail());
            }

            @Test
            @DisplayName("Error response with 'error' field format parses error and error_description")
            void errorFieldFormat() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_token\",\"error_description\":\"Token has been revoked\"}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals("invalid_token", ex.getErrorCode());
                assertEquals("Token has been revoked", ex.getMessage());
            }

            @Test
            @DisplayName("Error response body is not valid JSON includes '(response body not valid JSON)'")
            void errorResponseNotValidJson() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>Server Error</html>")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(500, ex.getStatusCode());
                assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
            }
        }

        @Nested
        @DisplayName("Network error cases")
        class NetworkErrors {

            @Test
            @DisplayName("Connection refused (wrong port) throws IO_ERROR exception")
            void connectionRefused() {
                TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
                // Port 1 is almost certainly not listening
                try (TwitterClient badClient = TwitterClient.builder()
                        .config(config)
                        .apiBaseUrl("http://127.0.0.1:1")
                        .tokenUrl("http://127.0.0.1:1/oauth2/token")
                        .authBaseUrl("http://127.0.0.1:1/authorize")
                        .build()) {

                    TwitterApiException ex = assertThrows(TwitterApiException.class,
                        () -> badClient.postTweet(ACCESS_TOKEN, "hi"));

                    assertEquals(0, ex.getStatusCode());
                    assertEquals("IO_ERROR", ex.getErrorCode());
                }
            }

            @Test
            @DisplayName("First request fails with IOException, second succeeds (retry works)")
            void ioErrorThenSuccess() {
                // Use WireMock scenario: first call has a fault, second succeeds
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("ioRetry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                    .willSetStateTo("SUCCESS"));

                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .inScenario("ioRetry")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"777\",\"text\":\"recovered\"}}")));

                TweetResponse response = client.postTweet(ACCESS_TOKEN, "recovered");

                assertNotNull(response);
                assertEquals("777", response.id());
            }

            @Test
            @DisplayName("All retry attempts fail with IOException throws IO_ERROR")
            void allRetriesFail() {
                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.postTweet(ACCESS_TOKEN, "hi"));

                assertEquals(0, ex.getStatusCode());
                assertEquals("IO_ERROR", ex.getErrorCode());
            }
        }
    }

    // ========================================================================
    // deleteTweet
    // ========================================================================
    @Nested
    @DisplayName("deleteTweet")
    class DeleteTweetTests {

        @Nested
        @DisplayName("Normal cases")
        class NormalCases {

            @Test
            @DisplayName("Delete success does not throw exception")
            void deleteSuccess() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/456"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"deleted\":true}}")));

                assertDoesNotThrow(() -> client.deleteTweet(ACCESS_TOKEN, "456"));
            }

            @Test
            @DisplayName("Verify DELETE method is used")
            void verifyDeleteMethod() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/789"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\":{\"deleted\":true}}")));

                client.deleteTweet(ACCESS_TOKEN, "789");

                wireMock.verify(deleteRequestedFor(urlEqualTo("/tweets/789")));
            }

            @Test
            @DisplayName("Verify Authorization header is sent")
            void verifyAuthHeader() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/111"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\":{\"deleted\":true}}")));

                client.deleteTweet(ACCESS_TOKEN, "111");

                wireMock.verify(deleteRequestedFor(urlEqualTo("/tweets/111"))
                    .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
            }

            @Test
            @DisplayName("Verify URL contains tweet ID")
            void verifyUrlContainsTweetId() {
                String tweetId = "specific-tweet-id-12345";
                wireMock.stubFor(delete(urlEqualTo("/tweets/" + tweetId))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\":{\"deleted\":true}}")));

                client.deleteTweet(ACCESS_TOKEN, tweetId);

                wireMock.verify(deleteRequestedFor(urlEqualTo("/tweets/" + tweetId)));
            }
        }

        @Nested
        @DisplayName("Error cases")
        class ErrorCases {

            @Test
            @DisplayName("400 error throws exception")
            void badRequest() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/bad"))
                    .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Bad Request\",\"type\":\"invalid_request\",\"detail\":\"Invalid tweet ID\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "bad"));

                assertEquals(400, ex.getStatusCode());
            }

            @Test
            @DisplayName("401 error throws exception")
            void unauthorized() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/123"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Unauthorized\",\"type\":\"unauthorized\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "123"));

                assertEquals(401, ex.getStatusCode());
            }

            @Test
            @DisplayName("403 error throws exception with parsed message")
            void forbidden() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/123"))
                    .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Forbidden\",\"type\":\"forbidden\",\"detail\":\"You cannot delete this tweet\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "123"));

                assertEquals(403, ex.getStatusCode());
                assertEquals("Forbidden", ex.getMessage());
                assertEquals("You cannot delete this tweet", ex.getDetail());
            }

            @Test
            @DisplayName("404 Not Found throws exception")
            void notFound() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/nonexistent"))
                    .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Not Found\",\"type\":\"not_found\",\"detail\":\"Tweet not found\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "nonexistent"));

                assertEquals(404, ex.getStatusCode());
            }

            @Test
            @DisplayName("429 then success works with retry")
            void rateLimitThenSuccess() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/retry"))
                    .inScenario("deleteRetry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1")
                        .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                    .willSetStateTo("SUCCESS"));

                wireMock.stubFor(delete(urlEqualTo("/tweets/retry"))
                    .inScenario("deleteRetry")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"data\":{\"deleted\":true}}")));

                assertDoesNotThrow(() -> client.deleteTweet(ACCESS_TOKEN, "retry"));
            }

            @Test
            @DisplayName("500 error throws exception")
            void serverError() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/123"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Internal Server Error\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "123"));

                assertEquals(500, ex.getStatusCode());
            }

            @Test
            @DisplayName("Invalid JSON error body throws exception with fallback message")
            void invalidJsonErrorBody() {
                wireMock.stubFor(delete(urlEqualTo("/tweets/123"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("plain text error")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.deleteTweet(ACCESS_TOKEN, "123"));

                assertEquals(500, ex.getStatusCode());
                assertTrue(ex.getMessage().contains("(response body not valid JSON)"));
            }
        }
    }

    // ========================================================================
    // getMe
    // ========================================================================
    @Nested
    @DisplayName("getMe")
    class GetMeTests {

        @Nested
        @DisplayName("Normal cases")
        class NormalCases {

            @Test
            @DisplayName("Full response with all fields verifies all TwitterUser fields")
            void fullResponse_allFields() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "id": "user-42",
                                "username": "testuser",
                                "name": "Test User",
                                "profile_image_url": "https://pbs.twimg.com/profile_images/example.jpg",
                                "public_metrics": {
                                  "followers_count": 1500,
                                  "following_count": 300
                                }
                              }
                            }
                            """)));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertNotNull(user);
                assertEquals("user-42", user.id());
                assertEquals("testuser", user.username());
                assertEquals("Test User", user.name());
                assertEquals("https://pbs.twimg.com/profile_images/example.jpg", user.profileImageUrl());
                assertEquals(1500, user.followersCount());
                assertEquals(300, user.followingCount());
            }

            @Test
            @DisplayName("Response without profile_image_url returns null for that field")
            void missingProfileImageUrl() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "id": "u1",
                                "username": "noavatar",
                                "name": "No Avatar",
                                "public_metrics": {"followers_count": 10, "following_count": 5}
                              }
                            }
                            """)));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertNull(user.profileImageUrl());
            }

            @Test
            @DisplayName("Response without public_metrics returns 0 for follower/following counts")
            void missingPublicMetrics() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "id": "u2",
                                "username": "nometrics",
                                "name": "No Metrics"
                              }
                            }
                            """)));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertEquals(0, user.followersCount());
                assertEquals(0, user.followingCount());
            }

            @Test
            @DisplayName("public_metrics with zero values returns correct zeros")
            void publicMetricsZero() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "id": "u3",
                                "username": "zerometrics",
                                "name": "Zero Metrics",
                                "public_metrics": {"followers_count": 0, "following_count": 0}
                              }
                            }
                            """)));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertEquals(0, user.followersCount());
                assertEquals(0, user.followingCount());
            }

            @Test
            @DisplayName("Response with extra unknown fields is ignored")
            void extraFieldsIgnored() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "id": "u4",
                                "username": "extra",
                                "name": "Extra Fields",
                                "profile_image_url": "https://img.example.com/pic.jpg",
                                "public_metrics": {"followers_count": 5, "following_count": 3, "tweet_count": 100},
                                "verified": true,
                                "location": "Tokyo"
                              },
                              "includes": {"something": "else"}
                            }
                            """)));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertEquals("u4", user.id());
                assertEquals("Extra Fields", user.name());
            }
        }

        @Nested
        @DisplayName("Request verification")
        class RequestVerification {

            @Test
            @DisplayName("Verify GET method is used")
            void verifyGetMethod() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"username\":\"u\",\"name\":\"n\"}}")));

                client.getMe(ACCESS_TOKEN);

                wireMock.verify(getRequestedFor(urlPathEqualTo("/users/me")));
            }

            @Test
            @DisplayName("Verify URL includes user.fields query param")
            void verifyUserFieldsQueryParam() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"username\":\"u\",\"name\":\"n\"}}")));

                client.getMe(ACCESS_TOKEN);

                wireMock.verify(getRequestedFor(urlPathEqualTo("/users/me"))
                    .withQueryParam("user.fields", equalTo("profile_image_url,public_metrics")));
            }

            @Test
            @DisplayName("Verify Authorization header is sent")
            void verifyAuthHeader() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"username\":\"u\",\"name\":\"n\"}}")));

                client.getMe(ACCESS_TOKEN);

                wireMock.verify(getRequestedFor(urlPathEqualTo("/users/me"))
                    .withHeader("Authorization", equalTo("Bearer " + ACCESS_TOKEN)));
            }
        }

        @Nested
        @DisplayName("Error cases")
        class ErrorCases {

            @Test
            @DisplayName("Missing 'data' field throws exception")
            void missingDataField() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals(200, ex.getStatusCode());
                assertEquals("UNKNOWN", ex.getErrorCode());
                assertTrue(ex.getMessage().contains("missing 'data' field"));
            }

            @Test
            @DisplayName("data: null returns TwitterUser with empty/null fields (NullNode passes null check)")
            void dataNull() {
                // Jackson's NullNode is not Java null, so the null check doesn't catch it.
                // The code proceeds to extract fields from a NullNode, returning defaults.
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":null}")));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                // NullNode.path("id").asText() returns ""
                assertNotNull(user);
                assertEquals("", user.id());
                assertEquals("", user.username());
            }

            @Test
            @DisplayName("401 Unauthorized throws exception")
            void unauthorized() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Unauthorized\",\"type\":\"unauthorized\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals(401, ex.getStatusCode());
            }

            @Test
            @DisplayName("403 Forbidden throws exception")
            void forbidden() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Forbidden\",\"type\":\"forbidden\",\"detail\":\"Not allowed\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals(403, ex.getStatusCode());
                assertEquals("Forbidden", ex.getMessage());
            }

            @Test
            @DisplayName("Invalid JSON response throws PARSE_ERROR")
            void invalidJson() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not valid json")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals("PARSE_ERROR", ex.getErrorCode());
            }

            @Test
            @DisplayName("Empty response body throws UNKNOWN exception (missing data field)")
            void emptyBody() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals("UNKNOWN", ex.getErrorCode());
                assertTrue(ex.getMessage().contains("missing 'data' field"));
            }

            @Test
            @DisplayName("429 then success works with retry")
            void rateLimitThenSuccess() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .inScenario("getMeRetry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1")
                        .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                    .willSetStateTo("SUCCESS"));

                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .inScenario("getMeRetry")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"username\":\"u\",\"name\":\"n\"}}")));

                TwitterUser user = client.getMe(ACCESS_TOKEN);

                assertNotNull(user);
                assertEquals("1", user.id());
            }

            @Test
            @DisplayName("500 error throws exception")
            void serverError() {
                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Server Error\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.getMe(ACCESS_TOKEN));

                assertEquals(500, ex.getStatusCode());
            }
        }
    }

    // ========================================================================
    // exchangeToken
    // ========================================================================
    @Nested
    @DisplayName("exchangeToken")
    class ExchangeTokenTests {

        @Nested
        @DisplayName("Normal cases")
        class NormalCases {

            @Test
            @DisplayName("Success returns all TokenResponse fields")
            void success_allFields() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "access_token": "at-123",
                              "refresh_token": "rt-456",
                              "expires_in": 7200,
                              "scope": "tweet.read tweet.write"
                            }
                            """)));

                TokenResponse response = client.exchangeToken("auth-code", "code-verifier");

                assertNotNull(response);
                assertEquals("at-123", response.accessToken());
                assertEquals("rt-456", response.refreshToken());
                assertEquals(7200, response.expiresIn());
                assertEquals("tweet.read tweet.write", response.scope());
            }

            @Test
            @DisplayName("Response with extra fields is ignored")
            void extraFieldsIgnored() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "access_token": "at",
                              "refresh_token": "rt",
                              "expires_in": 3600,
                              "scope": "read",
                              "token_type": "bearer",
                              "unknown_field": "value"
                            }
                            """)));

                TokenResponse response = client.exchangeToken("code", "verifier");

                assertEquals("at", response.accessToken());
            }
        }

        @Nested
        @DisplayName("Request verification")
        class RequestVerification {

            private void stubTokenSuccess() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"scope\":\"r\"}")));
            }

            @Test
            @DisplayName("Verify POST method is used")
            void verifyPostMethod() {
                stubTokenSuccess();

                client.exchangeToken("code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token")));
            }

            @Test
            @DisplayName("Verify Content-Type is x-www-form-urlencoded")
            void verifyContentType() {
                stubTokenSuccess();

                client.exchangeToken("code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
            }

            @Test
            @DisplayName("Verify body contains grant_type=authorization_code")
            void verifyGrantType() {
                stubTokenSuccess();

                client.exchangeToken("code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("grant_type=authorization_code")));
            }

            @Test
            @DisplayName("Verify body contains code parameter")
            void verifyCodeParam() {
                stubTokenSuccess();

                client.exchangeToken("my-auth-code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("code=my-auth-code")));
            }

            @Test
            @DisplayName("Verify body contains redirect_uri parameter")
            void verifyRedirectUri() {
                stubTokenSuccess();

                client.exchangeToken("code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("redirect_uri=")));
            }

            @Test
            @DisplayName("Verify body contains code_verifier parameter")
            void verifyCodeVerifier() {
                stubTokenSuccess();

                client.exchangeToken("code", "my-verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("code_verifier=my-verifier")));
            }

            @Test
            @DisplayName("Verify body contains client_id parameter")
            void verifyClientId() {
                stubTokenSuccess();

                client.exchangeToken("code", "verifier");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("client_id=" + CLIENT_ID)));
            }
        }

        @Nested
        @DisplayName("Error cases")
        class ErrorCases {

            @Test
            @DisplayName("400 with error/error_description format throws exception")
            void badRequestWithErrorField() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_request\",\"error_description\":\"Missing required parameter\"}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.exchangeToken("code", "verifier"));

                assertEquals(400, ex.getStatusCode());
                assertEquals("invalid_request", ex.getErrorCode());
                assertEquals("Missing required parameter", ex.getMessage());
            }

            @Test
            @DisplayName("401 invalid_grant throws exception")
            void invalidGrant() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Authorization code expired\"}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.exchangeToken("expired-code", "verifier"));

                assertEquals(401, ex.getStatusCode());
                assertEquals("invalid_grant", ex.getErrorCode());
                assertEquals("Authorization code expired", ex.getMessage());
            }

            @Test
            @DisplayName("Invalid JSON response throws PARSE_ERROR")
            void invalidJsonResponse() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("not json")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.exchangeToken("code", "verifier"));

                assertEquals("PARSE_ERROR", ex.getErrorCode());
            }

            @Test
            @DisplayName("Empty response body throws PARSE_ERROR")
            void emptyResponse() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.exchangeToken("code", "verifier"));

                assertEquals("PARSE_ERROR", ex.getErrorCode());
            }

            @Test
            @DisplayName("429 then success works with retry")
            void rateLimitThenSuccess() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .inScenario("tokenRetry")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1")
                        .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                    .willSetStateTo("SUCCESS"));

                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .inScenario("tokenRetry")
                    .whenScenarioStateIs("SUCCESS")
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"scope\":\"r\"}")));

                TokenResponse response = client.exchangeToken("code", "verifier");

                assertNotNull(response);
                assertEquals("at", response.accessToken());
            }
        }
    }

    // ========================================================================
    // refreshToken
    // ========================================================================
    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Nested
        @DisplayName("Normal cases")
        class NormalCases {

            @Test
            @DisplayName("Success returns all TokenResponse fields")
            void success() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "access_token": "new-at",
                              "refresh_token": "new-rt",
                              "expires_in": 7200,
                              "scope": "read write"
                            }
                            """)));

                TokenResponse response = client.refreshToken("old-refresh-token");

                assertNotNull(response);
                assertEquals("new-at", response.accessToken());
                assertEquals("new-rt", response.refreshToken());
                assertEquals(7200, response.expiresIn());
                assertEquals("read write", response.scope());
            }

            @Test
            @DisplayName("Response with extra fields is ignored")
            void extraFieldsIgnored() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"scope\":\"r\",\"token_type\":\"bearer\"}")));

                TokenResponse response = client.refreshToken("old-rt");

                assertEquals("at", response.accessToken());
            }
        }

        @Nested
        @DisplayName("Request verification")
        class RequestVerification {

            private void stubRefreshSuccess() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"scope\":\"r\"}")));
            }

            @Test
            @DisplayName("Verify body contains grant_type=refresh_token")
            void verifyGrantType() {
                stubRefreshSuccess();

                client.refreshToken("my-rt");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("grant_type=refresh_token")));
            }

            @Test
            @DisplayName("Verify body contains refresh_token parameter")
            void verifyRefreshTokenParam() {
                stubRefreshSuccess();

                client.refreshToken("my-refresh-token");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("refresh_token=my-refresh-token")));
            }

            @Test
            @DisplayName("Verify body contains client_id parameter")
            void verifyClientId() {
                stubRefreshSuccess();

                client.refreshToken("rt");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withRequestBody(containing("client_id=" + CLIENT_ID)));
            }

            @Test
            @DisplayName("Verify Content-Type is x-www-form-urlencoded")
            void verifyContentType() {
                stubRefreshSuccess();

                client.refreshToken("rt");

                wireMock.verify(postRequestedFor(urlEqualTo("/oauth2/token"))
                    .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")));
            }
        }

        @Nested
        @DisplayName("Error cases")
        class ErrorCases {

            @Test
            @DisplayName("400 invalid_grant throws exception")
            void invalidGrant() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"Refresh token expired\"}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.refreshToken("expired-rt"));

                assertEquals(400, ex.getStatusCode());
                assertEquals("invalid_grant", ex.getErrorCode());
                assertEquals("Refresh token expired", ex.getMessage());
            }

            @Test
            @DisplayName("401 Unauthorized throws exception")
            void unauthorized() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized_client\",\"error_description\":\"Client not authorized\"}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.refreshToken("rt"));

                assertEquals(401, ex.getStatusCode());
                assertEquals("unauthorized_client", ex.getErrorCode());
            }

            @Test
            @DisplayName("Invalid JSON response throws PARSE_ERROR")
            void invalidJson() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("garbage")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.refreshToken("rt"));

                assertEquals("PARSE_ERROR", ex.getErrorCode());
            }

            @Test
            @DisplayName("500 error throws exception")
            void serverError() {
                wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                    .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[{\"message\":\"Server Error\"}]}")));

                TwitterApiException ex = assertThrows(TwitterApiException.class,
                    () -> client.refreshToken("rt"));

                assertEquals(500, ex.getStatusCode());
            }
        }
    }

    // ========================================================================
    // getAuthorizationUrl
    // ========================================================================
    @Nested
    @DisplayName("getAuthorizationUrl")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("Contains response_type=code")
        void containsResponseType() {
            String url = client.getAuthorizationUrl("state123", "challenge456");
            assertTrue(url.contains("response_type=code"));
        }

        @Test
        @DisplayName("Contains client_id")
        void containsClientId() {
            String url = client.getAuthorizationUrl("state", "challenge");
            assertTrue(url.contains("client_id=" + CLIENT_ID));
        }

        @Test
        @DisplayName("Contains redirect_uri")
        void containsRedirectUri() {
            String url = client.getAuthorizationUrl("state", "challenge");
            // redirect_uri is URL-encoded
            assertTrue(url.contains("redirect_uri="));
            assertTrue(url.contains("localhost"));
        }

        @Test
        @DisplayName("Contains scope with tweet.read tweet.write users.read offline.access")
        void containsScope() {
            String url = client.getAuthorizationUrl("state", "challenge");
            assertTrue(url.contains("scope="));
            // URL-encoded spaces become +
            assertTrue(url.contains("tweet.read"));
            assertTrue(url.contains("tweet.write"));
            assertTrue(url.contains("users.read"));
            assertTrue(url.contains("offline.access"));
        }

        @Test
        @DisplayName("Contains state parameter")
        void containsState() {
            String url = client.getAuthorizationUrl("my-state-value", "challenge");
            assertTrue(url.contains("state=my-state-value"));
        }

        @Test
        @DisplayName("Contains code_challenge")
        void containsCodeChallenge() {
            String url = client.getAuthorizationUrl("state", "my-code-challenge");
            assertTrue(url.contains("code_challenge=my-code-challenge"));
        }

        @Test
        @DisplayName("Contains code_challenge_method=S256")
        void containsCodeChallengeMethod() {
            String url = client.getAuthorizationUrl("state", "challenge");
            assertTrue(url.contains("code_challenge_method=S256"));
        }

        @Test
        @DisplayName("Parameters are URL-encoded")
        void parametersAreUrlEncoded() {
            // redirect_uri contains :// and / which should be encoded
            String url = client.getAuthorizationUrl("state", "challenge");
            // The redirect URI http://localhost/callback should be encoded
            String encodedUri = "http%3A%2F%2Flocalhost%2Fcallback";
            assertTrue(url.contains("redirect_uri=" + encodedUri),
                "redirect_uri should be URL-encoded, got: " + url);
        }

        @Test
        @DisplayName("Special characters in state are URL-encoded")
        void specialCharsInStateEncoded() {
            String url = client.getAuthorizationUrl("state with spaces&special=chars", "challenge");
            // Space should be encoded as + or %20, & as %26, = as %3D
            assertTrue(!url.contains("state=state with spaces"),
                "Spaces in state should be encoded");
            assertTrue(url.contains("state=state"), "Should contain state param");
        }

        @Test
        @DisplayName("URL starts with auth base URL")
        void startsWithAuthBaseUrl() {
            String url = client.getAuthorizationUrl("state", "challenge");
            assertTrue(url.startsWith(wireMock.baseUrl() + "/authorize?"));
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================
    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Build without config throws IllegalStateException")
        void buildWithoutConfig() {
            assertThrows(IllegalStateException.class, () -> TwitterClient.builder().build());
        }

        @Test
        @DisplayName("Build without config shows correct message")
        void buildWithoutConfigMessage() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> TwitterClient.builder().build());
            assertEquals("TwitterConfig must be provided", ex.getMessage());
        }

        @Test
        @DisplayName("Build with config only uses defaults and works")
        void buildWithConfigOnly() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            try (TwitterClient c = TwitterClient.builder().config(config).build()) {
                assertNotNull(c);
                // Can generate an auth URL without error
                String url = c.getAuthorizationUrl("state", "challenge");
                assertNotNull(url);
            }
        }

        @Test
        @DisplayName("Build with custom HttpClient uses it")
        void buildWithCustomHttpClient() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            HttpClient customClient = HttpClient.newBuilder().build();
            try (TwitterClient c = TwitterClient.builder()
                    .config(config)
                    .httpClient(customClient)
                    .apiBaseUrl(wireMock.baseUrl())
                    .tokenUrl(wireMock.baseUrl() + "/oauth2/token")
                    .build()) {

                wireMock.stubFor(get(urlPathEqualTo("/users/me"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"username\":\"u\",\"name\":\"n\"}}")));

                TwitterUser user = c.getMe(ACCESS_TOKEN);
                assertNotNull(user);
            }
        }

        @Test
        @DisplayName("Build with custom ObjectMapper uses it")
        void buildWithCustomObjectMapper() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            ObjectMapper customMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try (TwitterClient c = TwitterClient.builder()
                    .config(config)
                    .objectMapper(customMapper)
                    .apiBaseUrl(wireMock.baseUrl())
                    .tokenUrl(wireMock.baseUrl() + "/oauth2/token")
                    .build()) {

                wireMock.stubFor(post(urlEqualTo("/tweets"))
                    .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"id\":\"1\",\"text\":\"test\"}}")));

                TweetResponse response = c.postTweet(ACCESS_TOKEN, "test");
                assertNotNull(response);
            }
        }

        @Test
        @DisplayName("close() on default HttpClient does not throw")
        void closeDefaultHttpClient() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            TwitterClient c = TwitterClient.builder().config(config).build();
            assertDoesNotThrow(c::close);
        }

        @Test
        @DisplayName("close() on custom HttpClient closes it without error")
        void closeCustomHttpClient() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            HttpClient customClient = HttpClient.newBuilder().build();
            TwitterClient c = TwitterClient.builder()
                .config(config)
                .httpClient(customClient)
                .build();
            assertDoesNotThrow(c::close);
        }

        @Test
        @DisplayName("Builder supports method chaining (fluent API)")
        void fluentApi() {
            TwitterConfig config = new TwitterConfig(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
            try (TwitterClient c = TwitterClient.builder()
                    .config(config)
                    .apiBaseUrl("http://localhost:9999")
                    .authBaseUrl("http://localhost:9999/auth")
                    .tokenUrl("http://localhost:9999/token")
                    .build()) {
                assertNotNull(c);
            }
        }
    }

    // ========================================================================
    // TwitterConfig validation
    // ========================================================================
    @Nested
    @DisplayName("TwitterConfig")
    class TwitterConfigTests {

        @Test
        @DisplayName("Null clientId throws IllegalArgumentException")
        void nullClientId() {
            assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig(null, "secret", "uri"));
        }

        @Test
        @DisplayName("Blank clientId throws IllegalArgumentException")
        void blankClientId() {
            assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("  ", "secret", "uri"));
        }

        @Test
        @DisplayName("Null clientSecret throws IllegalArgumentException")
        void nullClientSecret() {
            assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("id", null, "uri"));
        }

        @Test
        @DisplayName("Blank clientSecret throws IllegalArgumentException")
        void blankClientSecret() {
            assertThrows(IllegalArgumentException.class,
                () -> new TwitterConfig("id", "  ", "uri"));
        }

        @Test
        @DisplayName("Valid config creates successfully")
        void validConfig() {
            TwitterConfig config = new TwitterConfig("id", "secret", "http://localhost/callback");
            assertEquals("id", config.clientId());
            assertEquals("secret", config.clientSecret());
            assertEquals("http://localhost/callback", config.redirectUri());
        }

        @Test
        @DisplayName("Null redirectUri is allowed")
        void nullRedirectUriAllowed() {
            assertDoesNotThrow(() -> new TwitterConfig("id", "secret", null));
        }
    }

    // ========================================================================
    // OAuth2PkceFlow
    // ========================================================================
    @Nested
    @DisplayName("OAuth2PkceFlow")
    class OAuth2PkceFlowTests {

        @Test
        @DisplayName("generateCodeVerifier returns non-null non-empty string")
        void generateCodeVerifier_notEmpty() {
            String verifier = OAuth2PkceFlow.generateCodeVerifier();
            assertNotNull(verifier);
            assertTrue(verifier.length() > 0);
        }

        @Test
        @DisplayName("generateCodeVerifier returns Base64 URL-safe encoded string")
        void generateCodeVerifier_base64UrlSafe() {
            String verifier = OAuth2PkceFlow.generateCodeVerifier();
            // Base64 URL-safe uses [A-Za-z0-9_-] and no padding
            assertTrue(verifier.matches("[A-Za-z0-9_-]+"),
                "Code verifier should be Base64 URL-safe encoded: " + verifier);
        }

        @Test
        @DisplayName("generateCodeVerifier returns unique values")
        void generateCodeVerifier_unique() {
            String v1 = OAuth2PkceFlow.generateCodeVerifier();
            String v2 = OAuth2PkceFlow.generateCodeVerifier();
            assertTrue(!v1.equals(v2), "Two generated verifiers should be different");
        }

        @Test
        @DisplayName("generateCodeChallenge returns non-null non-empty string")
        void generateCodeChallenge_notEmpty() {
            String challenge = OAuth2PkceFlow.generateCodeChallenge("test-verifier");
            assertNotNull(challenge);
            assertTrue(challenge.length() > 0);
        }

        @Test
        @DisplayName("generateCodeChallenge is deterministic for same input")
        void generateCodeChallenge_deterministic() {
            String verifier = "fixed-verifier-value";
            String c1 = OAuth2PkceFlow.generateCodeChallenge(verifier);
            String c2 = OAuth2PkceFlow.generateCodeChallenge(verifier);
            assertEquals(c1, c2);
        }

        @Test
        @DisplayName("generateCodeChallenge returns different values for different verifiers")
        void generateCodeChallenge_differentInputs() {
            String c1 = OAuth2PkceFlow.generateCodeChallenge("verifier-one");
            String c2 = OAuth2PkceFlow.generateCodeChallenge("verifier-two");
            assertTrue(!c1.equals(c2));
        }

        @Test
        @DisplayName("generateCodeChallenge returns Base64 URL-safe encoded string")
        void generateCodeChallenge_base64UrlSafe() {
            String challenge = OAuth2PkceFlow.generateCodeChallenge("test-verifier");
            assertTrue(challenge.matches("[A-Za-z0-9_-]+"),
                "Code challenge should be Base64 URL-safe encoded: " + challenge);
        }

        @Test
        @DisplayName("Code verifier and challenge round-trip works correctly")
        void roundTrip() {
            String verifier = OAuth2PkceFlow.generateCodeVerifier();
            String challenge = OAuth2PkceFlow.generateCodeChallenge(verifier);
            assertNotNull(challenge);
            // SHA-256 hash is 32 bytes, Base64-encoded without padding = 43 chars
            assertEquals(43, challenge.length(),
                "SHA-256 Base64 URL-safe encoded should be 43 chars: " + challenge);
        }
    }

    // ========================================================================
    // Edge cases across methods
    // ========================================================================
    @Nested
    @DisplayName("Cross-cutting edge cases")
    class CrossCuttingEdgeCases {

        @Test
        @DisplayName("Error response with empty 'errors' array uses default message")
        void emptyErrorsArray() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"errors\":[]}")));

            TwitterApiException ex = assertThrows(TwitterApiException.class,
                () -> client.postTweet(ACCESS_TOKEN, "hi"));

            assertEquals(400, ex.getStatusCode());
            // Empty errors array means no first element, so falls through to default
            assertEquals("UNKNOWN", ex.getErrorCode());
            assertEquals("HTTP 400", ex.getMessage());
        }

        @Test
        @DisplayName("Error response with errors not array uses default or error field")
        void errorsNotArray() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"errors\":\"string instead of array\"}")));

            TwitterApiException ex = assertThrows(TwitterApiException.class,
                () -> client.postTweet(ACCESS_TOKEN, "hi"));

            assertEquals(400, ex.getStatusCode());
            // errors is not an array, and no "error" field, so defaults are used
            assertEquals("UNKNOWN", ex.getErrorCode());
        }

        @Test
        @DisplayName("Error with only 'error' field and no 'error_description' uses HTTP status as message")
        void errorFieldWithoutDescription() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"bad_request\"}")));

            TwitterApiException ex = assertThrows(TwitterApiException.class,
                () -> client.postTweet(ACCESS_TOKEN, "hi"));

            assertEquals("bad_request", ex.getErrorCode());
            // error_description missing => defaults to "HTTP 400"
            assertEquals("HTTP 400", ex.getMessage());
        }

        @Test
        @DisplayName("Multiple 429 retries with exponential backoff then success")
        void multiple429ThenSuccess() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .inScenario("multiRetry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")
                    .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                .willSetStateTo("RETRY_2"));

            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .inScenario("multiRetry")
                .whenScenarioStateIs("RETRY_2")
                .willReturn(aResponse()
                    .withStatus(429)
                    .withHeader("Retry-After", "1")
                    .withBody("{\"errors\":[{\"message\":\"Still rate limited\"}]}"))
                .willSetStateTo("SUCCESS"));

            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .inScenario("multiRetry")
                .whenScenarioStateIs("SUCCESS")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"data\":{\"id\":\"42\",\"text\":\"finally\"}}")));

            TweetResponse response = client.postTweet(ACCESS_TOKEN, "finally");
            assertNotNull(response);
            assertEquals("42", response.id());
        }

        @Test
        @DisplayName("IO error retry with eventual success")
        void ioRetryThenSuccess_delete() {
            wireMock.stubFor(delete(urlEqualTo("/tweets/io-retry"))
                .inScenario("deleteIoRetry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("SUCCESS"));

            wireMock.stubFor(delete(urlEqualTo("/tweets/io-retry"))
                .inScenario("deleteIoRetry")
                .whenScenarioStateIs("SUCCESS")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("{\"data\":{\"deleted\":true}}")));

            assertDoesNotThrow(() -> client.deleteTweet(ACCESS_TOKEN, "io-retry"));
        }

        @Test
        @DisplayName("429 without Retry-After header uses default delay")
        void rateLimitWithoutRetryAfter() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .inScenario("noRetryAfter")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                    .withStatus(429)
                    .withBody("{\"errors\":[{\"message\":\"Rate limited\"}]}"))
                .willSetStateTo("SUCCESS"));

            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .inScenario("noRetryAfter")
                .whenScenarioStateIs("SUCCESS")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"data\":{\"id\":\"1\",\"text\":\"ok\"}}")));

            long start = System.currentTimeMillis();
            TweetResponse response = client.postTweet(ACCESS_TOKEN, "ok");
            long elapsed = System.currentTimeMillis() - start;

            assertNotNull(response);
            // Without Retry-After, uses delayMs=1000
            assertTrue(elapsed >= 900, "Should wait at least ~1s with default delay");
        }

        @Test
        @DisplayName("Error response with errors array where first error has no type/message/detail uses defaults")
        void errorsArrayMinimalFields() {
            wireMock.stubFor(post(urlEqualTo("/tweets"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"errors\":[{\"title\":\"some error\"}]}")));

            TwitterApiException ex = assertThrows(TwitterApiException.class,
                () -> client.postTweet(ACCESS_TOKEN, "hi"));

            assertEquals(400, ex.getStatusCode());
            // message defaults to "HTTP 400" since "message" field is missing
            assertEquals("HTTP 400", ex.getMessage());
            // type defaults to "UNKNOWN"
            assertEquals("UNKNOWN", ex.getErrorCode());
        }
    }
}
