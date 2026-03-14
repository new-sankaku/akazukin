package com.akazukin.adapter.core;

import com.akazukin.domain.model.SnsPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RateLimiter Tests")
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Nested
    @DisplayName("checkLimit()")
    class CheckLimitTests {

        @Test
        @DisplayName("within limit does not throw")
        void withinLimit_doesNotThrow() {
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.MASTODON));
        }

        @Test
        @DisplayName("first call on any platform does not throw")
        void firstCall_doesNotThrow() {
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.TWITTER));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.BLUESKY));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.REDDIT));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.VK));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.PINTEREST));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.INSTAGRAM));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.THREADS));
        }

        @Test
        @DisplayName("Telegram limit is 30 - exceeding throws")
        void telegramLimit_30() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
        }

        @Test
        @DisplayName("Twitter limit is 50 - exceeding throws")
        void twitterLimit_50() {
            for (int i = 0; i < 50; i++) {
                rateLimiter.recordCall(SnsPlatform.TWITTER);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TWITTER));
        }

        @Test
        @DisplayName("Reddit limit is 60 - exceeding throws")
        void redditLimit_60() {
            for (int i = 0; i < 60; i++) {
                rateLimiter.recordCall(SnsPlatform.REDDIT);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.REDDIT));
        }

        @Test
        @DisplayName("Bluesky limit is 100 - exceeding throws")
        void blueskyLimit_100() {
            for (int i = 0; i < 100; i++) {
                rateLimiter.recordCall(SnsPlatform.BLUESKY);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.BLUESKY));
        }

        @Test
        @DisplayName("VK limit is 100 - exceeding throws")
        void vkLimit_100() {
            for (int i = 0; i < 100; i++) {
                rateLimiter.recordCall(SnsPlatform.VK);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.VK));
        }

        @Test
        @DisplayName("Pinterest limit is 100 - exceeding throws")
        void pinterestLimit_100() {
            for (int i = 0; i < 100; i++) {
                rateLimiter.recordCall(SnsPlatform.PINTEREST);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.PINTEREST));
        }

        @Test
        @DisplayName("Instagram limit is 200 - exceeding throws")
        void instagramLimit_200() {
            for (int i = 0; i < 200; i++) {
                rateLimiter.recordCall(SnsPlatform.INSTAGRAM);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.INSTAGRAM));
        }

        @Test
        @DisplayName("Threads limit is 200 - exceeding throws")
        void threadsLimit_200() {
            for (int i = 0; i < 200; i++) {
                rateLimiter.recordCall(SnsPlatform.THREADS);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.THREADS));
        }

        @Test
        @DisplayName("Mastodon limit is 300 - at 299 does not throw")
        void mastodonLimit_justBelow() {
            for (int i = 0; i < 299; i++) {
                rateLimiter.recordCall(SnsPlatform.MASTODON);
            }
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.MASTODON));
        }

        @Test
        @DisplayName("Mastodon limit is 300 - at 300 throws")
        void mastodonLimit_atLimit() {
            for (int i = 0; i < 300; i++) {
                rateLimiter.recordCall(SnsPlatform.MASTODON);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.MASTODON));
        }

        @Test
        @DisplayName("just below limit does not throw (Telegram 29)")
        void justBelowLimit_doesNotThrow() {
            for (int i = 0; i < 29; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
        }

        @Test
        @DisplayName("at exact limit throws (Telegram 30)")
        void atExactLimit_throws() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
        }

        @Test
        @DisplayName("exceed limit throws (Telegram 31)")
        void exceedLimit_throws() {
            for (int i = 0; i < 31; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
        }
    }

    @Nested
    @DisplayName("Platform independence")
    class PlatformIndependenceTests {

        @Test
        @DisplayName("filling Telegram does not affect Bluesky")
        void telegramDoesNotAffectBluesky() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.BLUESKY));
        }

        @Test
        @DisplayName("filling Twitter does not affect Mastodon")
        void twitterDoesNotAffectMastodon() {
            for (int i = 0; i < 50; i++) {
                rateLimiter.recordCall(SnsPlatform.TWITTER);
            }
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.MASTODON));
        }

        @Test
        @DisplayName("filling Reddit does not affect Instagram")
        void redditDoesNotAffectInstagram() {
            for (int i = 0; i < 60; i++) {
                rateLimiter.recordCall(SnsPlatform.REDDIT);
            }
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.INSTAGRAM));
        }

        @Test
        @DisplayName("multiple platforms can be filled independently")
        void multiplePlatformsIndependent() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            for (int i = 0; i < 50; i++) {
                rateLimiter.recordCall(SnsPlatform.TWITTER);
            }

            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TWITTER));
            assertDoesNotThrow(() -> rateLimiter.checkLimit(SnsPlatform.BLUESKY));
        }
    }

    @Nested
    @DisplayName("recordCall()")
    class RecordCallTests {

        @Test
        @DisplayName("single recordCall does not throw")
        void singleRecordCall_doesNotThrow() {
            assertDoesNotThrow(() -> rateLimiter.recordCall(SnsPlatform.BLUESKY));
        }

        @Test
        @DisplayName("recordCall increments count")
        void recordCallIncrementsCount() {
            // After recording 30 calls for Telegram (limit=30), checkLimit should throw
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
        }

        @Test
        @DisplayName("recordCall for unknown/new platform does not throw")
        void recordCallForAnyPlatform() {
            // All SnsPlatform values should work
            for (SnsPlatform platform : SnsPlatform.values()) {
                assertDoesNotThrow(() -> rateLimiter.recordCall(platform));
            }
        }
    }

    @Nested
    @DisplayName("RateLimitExceededException")
    class ExceptionTests {

        @Test
        @DisplayName("exception contains platform")
        void exceptionContainsPlatform() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
            assertEquals(SnsPlatform.TELEGRAM, ex.getPlatform());
        }

        @Test
        @DisplayName("exception contains retry after seconds")
        void exceptionContainsRetryAfter() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
            assertTrue(ex.getRetryAfterSeconds() > 0);
        }

        @Test
        @DisplayName("exception message contains platform name")
        void exceptionMessageContainsPlatformName() {
            for (int i = 0; i < 30; i++) {
                rateLimiter.recordCall(SnsPlatform.TELEGRAM);
            }
            RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLimit(SnsPlatform.TELEGRAM));
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("TELEGRAM"));
        }
    }
}
