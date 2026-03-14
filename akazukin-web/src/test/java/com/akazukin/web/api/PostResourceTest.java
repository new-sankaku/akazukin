package com.akazukin.web.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("PostResource API Tests")
class PostResourceTest {

    // =========================================================================
    // POST /api/v1/posts — Create Post
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/posts (create)")
    class CreatePostTests {

        @Test
        @DisplayName("Without auth - blocked (non-success status)")
        void createPost_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Hello world\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201 && status != 202
                    : "Expected non-success status for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000101", roles = "ADMIN")
        @DisplayName("Missing content (empty string) returns 400 INVALID_INPUT")
        void createPost_emptyContent_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000102", roles = "ADMIN")
        @DisplayName("Null content returns 400 INVALID_INPUT")
        void createPost_nullContent_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000103", roles = "ADMIN")
        @DisplayName("Whitespace-only content returns 400 INVALID_INPUT")
        void createPost_whitespaceContent_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"   \",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000104", roles = "ADMIN")
        @DisplayName("Empty platforms list returns 400 INVALID_INPUT")
        void createPost_emptyPlatforms_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Some content\",\"targetPlatforms\":[]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000105", roles = "ADMIN")
        @DisplayName("Missing platforms (null) returns 400 INVALID_INPUT")
        void createPost_nullPlatforms_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Some content\"}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000106", roles = "ADMIN")
        @DisplayName("No connected SNS account for platform returns 400 ACCOUNT_NOT_CONNECTED")
        void createPost_noConnectedAccount_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Test content\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ACCOUNT_NOT_CONNECTED"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000107", roles = "ADMIN")
        @DisplayName("No connected account for BLUESKY platform returns 400")
        void createPost_noBlueskyAccount_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Test bluesky\",\"targetPlatforms\":[\"BLUESKY\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ACCOUNT_NOT_CONNECTED"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000108", roles = "ADMIN")
        @DisplayName("Multiple platforms with no connected accounts returns 400")
        void createPost_multiplePlatforms_noAccounts_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Test multi\",\"targetPlatforms\":[\"TWITTER\",\"MASTODON\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ACCOUNT_NOT_CONNECTED"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000109", roles = "ADMIN")
        @DisplayName("Null request body returns 400")
        void createPost_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000110", roles = "ADMIN")
        @DisplayName("Scheduled time in the past returns 400 INVALID_SCHEDULE")
        void createPost_scheduledInPast_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Scheduled past\",\"targetPlatforms\":[\"TWITTER\"],"
                            + "\"scheduledAt\":\"2020-01-01T00:00:00Z\"}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000111", roles = "ADMIN")
        @DisplayName("Invalid platform name returns error")
        void createPost_invalidPlatform_returnsError() {
            // SnsPlatform.valueOf will throw IllegalArgumentException
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Test\",\"targetPlatforms\":[\"NONEXISTENT_PLATFORM\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(not(equalTo(200)));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000112", roles = "ADMIN")
        @DisplayName("Content with special characters and emoji is accepted")
        void createPost_specialCharsAndEmoji() {
            // This will still fail due to no connected account, but the content validation should pass
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Hello <world> & 'test' \\\"quotes\\\"\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ACCOUNT_NOT_CONNECTED"));
        }
    }

    // =========================================================================
    // GET /api/v1/posts — List Posts
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/posts (list)")
    class ListPostsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000201", roles = "ADMIN")
        @DisplayName("Empty list returns 200 with empty array")
        void listPosts_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000202", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listPosts_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000203", roles = "ADMIN")
        @DisplayName("Default pagination (page=0, size=20) returns 200")
        void listPosts_defaultPagination() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000204", roles = "ADMIN")
        @DisplayName("Large page number returns empty list")
        void listPosts_largePageNumber() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 9999)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000205", roles = "ADMIN")
        @DisplayName("Page size 1 returns 200")
        void listPosts_pageSize1() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000206", roles = "ADMIN")
        @DisplayName("Page size 100 returns 200")
        void listPosts_largeSizePagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 100)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listPosts_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/posts/{id} — Get Post by ID
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/posts/{id}")
    class GetPostTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000301", roles = "ADMIN")
        @DisplayName("Non-existent UUID returns 404")
        void getPost_notFound() {
            UUID randomId = UUID.randomUUID();
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts/" + randomId)
            .then()
                    .statusCode(404);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000302", roles = "ADMIN")
        @DisplayName("Another random non-existent UUID also returns 404")
        void getPost_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getPost_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000303", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error (not 200)")
        void getPost_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/posts/{id} — Update Post
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/posts/{id}")
    class UpdatePostTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000401", roles = "ADMIN")
        @DisplayName("Non-existent post returns 404")
        void updatePost_notFound() {
            UUID randomId = UUID.randomUUID();
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Updated content\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .put("/api/v1/posts/" + randomId)
            .then()
                    .statusCode(404);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000402", roles = "ADMIN")
        @DisplayName("Null request body returns 400")
        void updatePost_nullBody() {
            UUID randomId = UUID.randomUUID();
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/posts/" + randomId)
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void updatePost_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"test\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .put("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000403", roles = "ADMIN")
        @DisplayName("Empty content in update returns appropriate error for non-existent post")
        void updatePost_emptyContent_nonExistentPost() {
            // Post doesn't exist, so 404 takes precedence
            UUID randomId = UUID.randomUUID();
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .put("/api/v1/posts/" + randomId)
            .then()
                    .statusCode(404);
        }
    }

    // =========================================================================
    // DELETE /api/v1/posts/{id} — Delete Post
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/posts/{id}")
    class DeletePostTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000501", roles = "ADMIN")
        @DisplayName("Non-existent post returns 404")
        void deletePost_notFound() {
            UUID randomId = UUID.randomUUID();
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/posts/" + randomId)
            .then()
                    .statusCode(404);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000502", roles = "ADMIN")
        @DisplayName("Deleting another random non-existent post returns 404")
        void deletePost_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deletePost_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success status for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000503", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void deletePost_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/posts/invalid-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // Role-based access tests
    // =========================================================================

    @Nested
    @DisplayName("Role-based access control")
    class RoleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000601", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void listPosts_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            // PostResource requires ADMIN role
            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000000602", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed (requires ADMIN)")
        void listPosts_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }
    }
}
