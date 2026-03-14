// SECURITY: This test reads credentials from environment variables. NEVER hardcode API keys.
package com.akazukin.sdk.twitter;

import com.akazukin.sdk.twitter.exception.TwitterApiException;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Twitter API integration tests.
 *
 * <p>These tests hit the ACTUAL Twitter API v2 using the production {@link TwitterClient} code.
 * All credentials are read from environment variables — nothing is hardcoded.</p>
 *
 * <p>Required environment variables:</p>
 * <ul>
 *   <li>{@code TWITTER_BEARER_TOKEN} — App-only Bearer Token (read-only operations)</li>
 *   <li>{@code TWITTER_CONSUMER_KEY} — Consumer Key / API Key</li>
 *   <li>{@code TWITTER_CONSUMER_SECRET} — Consumer Secret / API Secret</li>
 *   <li>{@code TWITTER_ACCESS_TOKEN} — User Access Token (OAuth 1.0a)</li>
 *   <li>{@code TWITTER_ACCESS_TOKEN_SECRET} — User Access Token Secret (OAuth 1.0a)</li>
 * </ul>
 *
 * <p>Run with: {@code ./gradlew :akazukin-sdk:akazukin-sdk-twitter:test -DincludeTags=real-api}</p>
 */
@Tag("real-api")
@EnabledIfEnvironmentVariable(named = "TWITTER_BEARER_TOKEN", matches = ".+")
class TwitterRealApiTest {

    private static TwitterClient client;
    private static String bearerToken;
    private static String accessToken;
    private static String accessTokenSecret;

    @BeforeAll
    static void setUp() {
        bearerToken = System.getenv("TWITTER_BEARER_TOKEN");
        String consumerKey = System.getenv("TWITTER_CONSUMER_KEY");
        String consumerSecret = System.getenv("TWITTER_CONSUMER_SECRET");
        accessToken = System.getenv("TWITTER_ACCESS_TOKEN");
        accessTokenSecret = System.getenv("TWITTER_ACCESS_TOKEN_SECRET");

        assertNotNull(bearerToken, "TWITTER_BEARER_TOKEN must be set");
        assertNotNull(consumerKey, "TWITTER_CONSUMER_KEY must be set");
        assertNotNull(consumerSecret, "TWITTER_CONSUMER_SECRET must be set");

        client = TwitterClient.builder()
            .config(new TwitterConfig(consumerKey, consumerSecret, "https://localhost/callback"))
            .build();
    }

    // =========================================================================
    // getTweetById — Bearer Token (app-only, read-only)
    // =========================================================================
    @Nested
    @DisplayName("getTweetById — 実API")
    class GetTweetByIdTests {

        @Test
        @DisplayName("既知のツイートID(20)を取得できる")
        void knownTweetId_returnsValidTweet() {
            TweetResponse tweet = client.getTweetById(bearerToken, "20");

            assertNotNull(tweet);
            assertEquals("20", tweet.id());
            assertNotNull(tweet.text());
            assertFalse(tweet.text().isBlank(), "ツイート本文が空であってはならない");
        }

        @Test
        @DisplayName("レスポンスのTweetResponseがid/textの両フィールドを持つ")
        void response_hasCorrectFields() {
            TweetResponse tweet = client.getTweetById(bearerToken, "20");

            assertNotNull(tweet.id(), "idがnullであってはならない");
            assertNotNull(tweet.text(), "textがnullであってはならない");
        }

        @Test
        @DisplayName("存在しないツイートIDでTwitterApiExceptionが投げられる")
        void nonExistentTweetId_throwsException() {
            assertThrows(TwitterApiException.class, () ->
                client.getTweetById(bearerToken, "999999999999999999999"));
        }

        @Test
        @DisplayName("不正なBearerTokenでTwitterApiExceptionが投げられる")
        void invalidToken_throwsException() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.getTweetById("invalid_token_value", "20"));

            assertTrue(ex.getStatusCode() == 401 || ex.getStatusCode() == 403,
                "不正トークンは401または403を返すはず。実際: " + ex.getStatusCode());
        }
    }

    // =========================================================================
    // getUserByUsername — Bearer Token (app-only, read-only)
    // =========================================================================
    @Nested
    @DisplayName("getUserByUsername — 実API")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("既知のユーザー名のプロフィールを取得できる")
        void knownUsername_returnsValidUser() {
            TwitterUser user = client.getUserByUsername(bearerToken, "elonmusk");

            assertNotNull(user);
            assertNotNull(user.id());
            assertFalse(user.id().isBlank(), "IDが空であってはならない");
            assertNotNull(user.username());
            assertFalse(user.username().isBlank(), "usernameが空であってはならない");
        }

        @Test
        @DisplayName("フォロワー数とフォロー数が0以上で返る")
        void knownUsername_hasPublicMetrics() {
            TwitterUser user = client.getUserByUsername(bearerToken, "elonmusk");

            assertTrue(user.followersCount() >= 0, "フォロワー数は0以上");
            assertTrue(user.followingCount() >= 0, "フォロー数は0以上");
        }

        @Test
        @DisplayName("プロフィール画像URLが返る（設定されている場合）")
        void knownUsername_hasProfileImage() {
            TwitterUser user = client.getUserByUsername(bearerToken, "elonmusk");

            if (user.profileImageUrl() != null) {
                assertTrue(user.profileImageUrl().startsWith("http"),
                    "プロフィール画像URLはhttpで始まるはず");
            }
        }

        @Test
        @DisplayName("存在しないユーザー名でTwitterApiExceptionが投げられる")
        void nonExistentUsername_throwsException() {
            assertThrows(TwitterApiException.class, () ->
                client.getUserByUsername(bearerToken, "zzzzzzzzzzznonexistent99999"));
        }

        @Test
        @DisplayName("不正なBearerTokenでTwitterApiExceptionが投げられる")
        void invalidToken_throwsException() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.getUserByUsername("invalid_token_value", "elonmusk"));

            assertTrue(ex.getStatusCode() == 401 || ex.getStatusCode() == 403,
                "不正トークンは401または403を返すはず。実際: " + ex.getStatusCode());
        }

        @Test
        @DisplayName("複数のユーザーを連続で取得できる")
        void multipleUsers_canBeFetchedSequentially() {
            TwitterUser user1 = client.getUserByUsername(bearerToken, "elonmusk");
            TwitterUser user2 = client.getUserByUsername(bearerToken, "nasa");

            assertNotNull(user1);
            assertNotNull(user2);
            assertFalse(user1.id().equals(user2.id()), "異なるユーザーのIDは異なるはず");
        }
    }

    // =========================================================================
    // getMe — OAuth 1.0a (ユーザーコンテキスト)
    // =========================================================================
    @Nested
    @DisplayName("getMe — OAuth 1.0a 実API")
    @EnabledIfEnvironmentVariable(named = "TWITTER_ACCESS_TOKEN", matches = ".+")
    class GetMeOAuth1Tests {

        @Test
        @DisplayName("OAuth 1.0aで自分のプロフィールを取得できる")
        void oauth1_returnsMyProfile() {
            TwitterUser me = client.getMe(accessToken, accessTokenSecret);

            assertNotNull(me);
            assertNotNull(me.id());
            assertFalse(me.id().isBlank(), "自分のIDが空であってはならない");
            assertNotNull(me.username());
            assertFalse(me.username().isBlank(), "自分のusernameが空であってはならない");
        }

        @Test
        @DisplayName("自分のフォロワー数・フォロー数が取得できる")
        void oauth1_hasPublicMetrics() {
            TwitterUser me = client.getMe(accessToken, accessTokenSecret);

            assertTrue(me.followersCount() >= 0, "フォロワー数は0以上");
            assertTrue(me.followingCount() >= 0, "フォロー数は0以上");
        }
    }

    // =========================================================================
    // postTweet + deleteTweet — OAuth 1.0a (実際の投稿→確認→削除)
    // =========================================================================
    @Nested
    @DisplayName("postTweet + deleteTweet — OAuth 1.0a 実API（投稿→確認→削除）")
    @EnabledIfEnvironmentVariable(named = "TWITTER_ACCESS_TOKEN", matches = ".+")
    class PostAndDeleteTweetTests {

        @Test
        @DisplayName("ツイートを投稿し、取得で確認し、削除できる")
        void postTweet_thenVerify_thenDelete() {
            String tweetText = "[Akazukin test] " + Instant.now() + " — this will be deleted";

            // 1. 投稿
            TweetResponse posted = client.postTweet(accessToken, accessTokenSecret, tweetText);
            assertNotNull(posted, "投稿レスポンスがnullであってはならない");
            assertNotNull(posted.id(), "投稿されたツイートIDがnullであってはならない");
            assertFalse(posted.id().isBlank(), "投稿されたツイートIDが空であってはならない");

            try {
                // 2. Bearer Tokenで投稿を取得して確認
                TweetResponse fetched = client.getTweetById(bearerToken, posted.id());
                assertNotNull(fetched, "取得したツイートがnullであってはならない");
                assertEquals(posted.id(), fetched.id(), "取得したIDが投稿時のIDと一致するはず");
                assertTrue(fetched.text().contains("[Akazukin test]"),
                    "ツイート本文にテスト識別子が含まれるはず");
            } finally {
                // 3. 削除（テスト成功/失敗に関わらず必ず削除）
                client.deleteTweet(accessToken, accessTokenSecret, posted.id());
            }

            // 4. 削除後に取得するとエラーになる
            assertThrows(TwitterApiException.class, () ->
                client.getTweetById(bearerToken, posted.id()),
                "削除済みツイートの取得はエラーになるはず");
        }

        @Test
        @DisplayName("投稿レスポンスにidとtextが含まれる")
        void postTweet_responseHasIdAndText() {
            String tweetText = "[Akazukin test] fields check " + Instant.now();

            TweetResponse posted = client.postTweet(accessToken, accessTokenSecret, tweetText);
            try {
                assertNotNull(posted.id(), "idフィールドが存在するはず");
                assertNotNull(posted.text(), "textフィールドが存在するはず");
                assertFalse(posted.text().isBlank(), "textが空であってはならない");
            } finally {
                client.deleteTweet(accessToken, accessTokenSecret, posted.id());
            }
        }

        @Test
        @DisplayName("不正なアクセストークンで投稿するとTwitterApiExceptionが投げられる")
        void invalidAccessToken_throwsException() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.postTweet("invalid_token", "invalid_secret", "should fail"));

            assertTrue(ex.getStatusCode() == 401 || ex.getStatusCode() == 403,
                "不正トークンは401または403を返すはず。実際: " + ex.getStatusCode());
        }

    }

    // =========================================================================
    // getAuthorizationUrl — ローカル生成、APIは叩かない
    // =========================================================================
    @Nested
    @DisplayName("getAuthorizationUrl — URL生成")
    class GetAuthorizationUrlTests {

        @Test
        @DisplayName("生成されたURLがTwitterのOAuth2認可エンドポイントを指す")
        void generatedUrl_pointsToTwitterAuth() {
            String url = client.getAuthorizationUrl("test-state", "test-challenge");

            assertTrue(url.startsWith("https://twitter.com/i/oauth2/authorize"),
                "TwitterのOAuth2認可URLで始まるはず");
        }

        @Test
        @DisplayName("URLにclient_id, state, code_challengeが含まれる")
        void generatedUrl_containsRequiredParams() {
            String url = client.getAuthorizationUrl("my-state", "my-challenge");

            assertTrue(url.contains("state=my-state"), "stateパラメータが含まれるはず");
            assertTrue(url.contains("code_challenge=my-challenge"), "code_challengeが含まれるはず");
            assertTrue(url.contains("response_type=code"), "response_type=codeが含まれるはず");
            assertTrue(url.contains("code_challenge_method=S256"), "PKCE S256メソッドが指定されるはず");
        }
    }

    // =========================================================================
    // エラーハンドリング — 実APIのエラーレスポンスが正しくパースされる
    // =========================================================================
    @Nested
    @DisplayName("エラーハンドリング — 実APIレスポンス")
    class ErrorHandlingTests {

        @Test
        @DisplayName("App-only BearerTokenではgetMeが403エラーを返す（ユーザーコンテキスト必要）")
        void appOnlyBearer_getMeThrowsForbidden() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.getMe(bearerToken));

            assertTrue(ex.getStatusCode() == 401 || ex.getStatusCode() == 403,
                "App-only tokenではgetMeは401または403になるはず。実際: " + ex.getStatusCode());
        }

        @Test
        @DisplayName("TwitterApiExceptionのerrorCodeがnullでない")
        void apiError_hasErrorCode() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.getMe(bearerToken));

            assertNotNull(ex.getErrorCode(), "errorCodeがnullであってはならない");
        }

        @Test
        @DisplayName("TwitterApiExceptionのdetailがnullでない")
        void apiError_hasDetail() {
            TwitterApiException ex = assertThrows(TwitterApiException.class, () ->
                client.getMe(bearerToken));

            assertNotNull(ex.getDetail(), "detailがnullであってはならない");
        }
    }

    // =========================================================================
    // クライアントのライフサイクル
    // =========================================================================
    @Nested
    @DisplayName("クライアントのライフサイクル")
    class ClientLifecycleTests {

        @Test
        @DisplayName("close()を呼んでもエラーにならない")
        void close_doesNotThrow() {
            TwitterClient tempClient = TwitterClient.builder()
                .config(new TwitterConfig("key", "secret", "https://localhost/cb"))
                .build();
            tempClient.close();
        }

        @Test
        @DisplayName("Builderがconfigなしでbuild()するとIllegalStateException")
        void builder_withoutConfig_throwsException() {
            assertThrows(IllegalStateException.class, () ->
                TwitterClient.builder().build());
        }
    }
}
