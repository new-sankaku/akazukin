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

@QuarkusTest
@DisplayName("NewsResource API Tests")
class NewsResourceTest {

    // =========================================================================
    // GET /api/v1/news/sources — List Sources
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/news/sources (list)")
    class ListSourcesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listSources_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/sources")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007002", roles = "USER")
        @DisplayName("USER role can list sources")
        void listSources_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/sources")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listSources_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/sources")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007003", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listSources_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/sources")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007004", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listSources_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/sources")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }
    }

    // =========================================================================
    // POST /api/v1/news/sources — Add Source
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/sources (add)")
    class AddSourceTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007101", roles = "ADMIN")
        @DisplayName("Valid request returns 201")
        void addSource_success() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Tech News\","
                            + "\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(201);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007102", roles = "ADMIN")
        @DisplayName("Missing name returns 400 INVALID_INPUT")
        void addSource_missingName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007103", roles = "ADMIN")
        @DisplayName("Empty name returns 400 INVALID_INPUT")
        void addSource_emptyName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"\","
                            + "\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007104", roles = "ADMIN")
        @DisplayName("Whitespace-only name returns 400 INVALID_INPUT")
        void addSource_whitespaceName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"   \","
                            + "\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007105", roles = "ADMIN")
        @DisplayName("Missing URL returns 400 INVALID_INPUT")
        void addSource_missingUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Tech News\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007106", roles = "ADMIN")
        @DisplayName("Empty URL returns 400 INVALID_INPUT")
        void addSource_emptyUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Tech News\","
                            + "\"url\":\"\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007107", roles = "ADMIN")
        @DisplayName("Missing sourceType defaults to RSS - returns 201")
        void addSource_missingSourceType_defaultsToRSS() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Default Type Source\","
                            + "\"url\":\"https://example.com/feed\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(201);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007108", roles = "USER")
        @DisplayName("USER role can add source")
        void addSource_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"User Source\","
                            + "\"url\":\"https://example.com/user-rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .statusCode(201);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void addSource_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\","
                            + "\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/news/sources/{id} — Remove Source
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/news/sources/{id}")
    class RemoveSourceTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007201", roles = "ADMIN")
        @DisplayName("Non-existent source returns 400 SOURCE_NOT_FOUND")
        void removeSource_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/news/sources/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("SOURCE_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007202", roles = "ADMIN")
        @DisplayName("Another non-existent source UUID also returns 400")
        void removeSource_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/news/sources/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("SOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void removeSource_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/news/sources/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007203", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void removeSource_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/news/sources/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/sources/{id}/generate — Generate Post from Source
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/sources/{id}/generate")
    class GeneratePostTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007301", roles = "ADMIN")
        @DisplayName("Non-existent source returns 400 SOURCE_NOT_FOUND")
        void generatePost_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/sources/" + UUID.randomUUID() + "/generate")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("SOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generatePost_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/sources/" + UUID.randomUUID() + "/generate")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/sources/{id}/fetch — Fetch Articles
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/sources/{id}/fetch")
    class FetchArticlesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007401", roles = "ADMIN")
        @DisplayName("Non-existent source returns 400 SOURCE_NOT_FOUND")
        void fetchArticles_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/sources/" + UUID.randomUUID() + "/fetch")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("SOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void fetchArticles_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/sources/" + UUID.randomUUID() + "/fetch")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/news/articles — List Articles
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/news/articles (list)")
    class ListArticlesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007501", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listArticles_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/articles")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007502", roles = "USER")
        @DisplayName("USER role can list articles")
        void listArticles_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/articles")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listArticles_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/articles")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007503", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listArticles_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/news/articles")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/post-idea — Generate Post Idea
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/post-idea")
    class GeneratePostIdeaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007601", roles = "ADMIN")
        @DisplayName("Non-existent newsItemId returns 400 NOT_FOUND")
        void generatePostIdea_newsItemNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"newsItemId\":\"" + UUID.randomUUID() + "\","
                            + "\"personaId\":\"" + UUID.randomUUID() + "\"}")
            .when()
                    .post("/api/v1/news/post-idea")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generatePostIdea_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"newsItemId\":\"" + UUID.randomUUID() + "\","
                            + "\"personaId\":\"" + UUID.randomUUID() + "\"}")
            .when()
                    .post("/api/v1/news/post-idea")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/articles/{id}/multi-angle — Multi-Angle Generation
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/articles/{id}/multi-angle")
    class MultiAngleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007701", roles = "ADMIN")
        @DisplayName("Non-existent newsItemId returns 400 NOT_FOUND")
        void generateMultiAngle_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/articles/" + UUID.randomUUID() + "/multi-angle")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generateMultiAngle_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/articles/" + UUID.randomUUID() + "/multi-angle")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007702", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void generateMultiAngle_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/articles/not-a-uuid/multi-angle")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/articles/{id}/template-match — Template Match
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/articles/{id}/template-match")
    class TemplateMatchTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007801", roles = "ADMIN")
        @DisplayName("Non-existent newsItemId returns 400 NOT_FOUND")
        void matchTemplates_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/articles/" + UUID.randomUUID() + "/template-match")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void matchTemplates_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/articles/" + UUID.randomUUID() + "/template-match")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/news/ab-test — Generate A/B Test
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/news/ab-test")
    class GenerateABTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007901", roles = "ADMIN")
        @DisplayName("Non-existent newsItemId returns 400 NOT_FOUND")
        void generateABTest_newsItemNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"newsItemId\":\"" + UUID.randomUUID() + "\","
                            + "\"personaIdA\":\"" + UUID.randomUUID() + "\","
                            + "\"personaIdB\":\"" + UUID.randomUUID() + "\"}")
            .when()
                    .post("/api/v1/news/ab-test")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generateABTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"newsItemId\":\"" + UUID.randomUUID() + "\","
                            + "\"personaIdA\":\"" + UUID.randomUUID() + "\","
                            + "\"personaIdB\":\"" + UUID.randomUUID() + "\"}")
            .when()
                    .post("/api/v1/news/ab-test")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // Role-based access tests
    // =========================================================================

    @Nested
    @DisplayName("Role-based access control")
    class RoleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007A01", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed for add source")
        void addSource_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\","
                            + "\"url\":\"https://example.com/rss\","
                            + "\"sourceType\":\"RSS\"}")
            .when()
                    .post("/api/v1/news/sources")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007A02", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed for delete source")
        void removeSource_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/news/sources/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007A03", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed for generate post")
        void generatePost_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/news/sources/" + UUID.randomUUID() + "/generate")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }
}
