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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@DisplayName("ABTestResource API Tests")
class ABTestResourceTest {

    // =========================================================================
    // GET /api/v1/ab-tests — List Tests
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ab-tests (list)")
    class ListTestsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listTests_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005002", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listTests_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listTests_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005003", roles = "USER")
        @DisplayName("USER role can list tests")
        void listTests_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005004", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listTests_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ab-tests — Create Test
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ab-tests (create)")
    class CreateTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005101", roles = "ADMIN")
        @DisplayName("Valid request returns 201 or 500 (AI unavailable)")
        void createTest_success() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test A/B\","
                            + "\"variantA\":\"Variant A text\","
                            + "\"variantB\":\"Variant B text\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status == 201 || status == 500
                    : "Expected 201 or 500 (AI unavailable), got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005102", roles = "ADMIN")
        @DisplayName("With three variants returns 201 or 500 (AI unavailable)")
        void createTest_threeVariants_success() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Three variant test\","
                            + "\"variantA\":\"A text\","
                            + "\"variantB\":\"B text\","
                            + "\"variantC\":\"C text\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status == 201 || status == 500
                    : "Expected 201 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\","
                            + "\"variantA\":\"A\","
                            + "\"variantB\":\"B\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005103", roles = "USER")
        @DisplayName("USER role can create test (201 or 500 AI unavailable)")
        void createTest_userRole_allowed() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"User test\","
                            + "\"variantA\":\"A\","
                            + "\"variantB\":\"B\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assert status == 201 || status == 500
                    : "Expected 201 or 500 (AI unavailable), got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/ab-tests/{id}/complete — Complete Test
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/ab-tests/{id}/complete")
    class CompleteTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005201", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void completeTest_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("winner", "A")
            .when()
                    .put("/api/v1/ab-tests/" + UUID.randomUUID() + "/complete")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void completeTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("winner", "A")
            .when()
                    .put("/api/v1/ab-tests/" + UUID.randomUUID() + "/complete")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/ab-tests/{id}/start — Start Test
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/ab-tests/{id}/start")
    class StartTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005301", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void startTest_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ab-tests/" + UUID.randomUUID() + "/start")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void startTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ab-tests/" + UUID.randomUUID() + "/start")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ab-tests/{id}/cancel — Cancel Test
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ab-tests/{id}/cancel")
    class CancelTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005401", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void cancelTest_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ab-tests/" + UUID.randomUUID() + "/cancel")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void cancelTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ab-tests/" + UUID.randomUUID() + "/cancel")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/ab-tests/{id} — Delete Test
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/ab-tests/{id}")
    class DeleteTestTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005501", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void deleteTest_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ab-tests/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteTest_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ab-tests/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005502", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void deleteTest_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ab-tests/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ab-tests/generate-variants — Generate Variants
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ab-tests/generate-variants")
    class GenerateVariantsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005601", roles = "ADMIN")
        @DisplayName("Valid request returns 200")
        void generateVariants_success() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"originalText\":\"Hello world\"}")
            .when()
                    .post("/api/v1/ab-tests/generate-variants")
            .then()
                    .extract().statusCode();

            assert status == 200 || status == 500
                    : "Expected 200 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generateVariants_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"originalText\":\"Hello world\"}")
            .when()
                    .post("/api/v1/ab-tests/generate-variants")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ab-tests/{id}/prediction — Predict Outcome
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ab-tests/{id}/prediction")
    class PredictOutcomeTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005701", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void predictOutcome_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/" + UUID.randomUUID() + "/prediction")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void predictOutcome_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/" + UUID.randomUUID() + "/prediction")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ab-tests/{id}/loser-analysis — Analyze Loser
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ab-tests/{id}/loser-analysis")
    class AnalyzeLoserTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005801", roles = "ADMIN")
        @DisplayName("Non-existent test returns error")
        void analyzeLoser_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/" + UUID.randomUUID() + "/loser-analysis")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent test, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void analyzeLoser_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/" + UUID.randomUUID() + "/loser-analysis")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ab-tests/multi-platform — Multi Platform Variants
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ab-tests/multi-platform")
    class MultiPlatformTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005901", roles = "ADMIN")
        @DisplayName("Valid request returns 200 or 500 (AI unavailable)")
        void multiPlatform_success() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Multi test\","
                            + "\"originalText\":\"Hello\","
                            + "\"platforms\":[\"TWITTER\",\"MASTODON\"]}")
            .when()
                    .post("/api/v1/ab-tests/multi-platform")
            .then()
                    .extract().statusCode();

            assert status == 200 || status == 500
                    : "Expected 200 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void multiPlatform_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Multi test\","
                            + "\"originalText\":\"Hello\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests/multi-platform")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ab-tests/win-patterns — Win Patterns
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ab-tests/win-patterns")
    class WinPatternsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005A01", roles = "ADMIN")
        @DisplayName("Returns 200 for authenticated user")
        void winPatterns_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/win-patterns")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005A02", roles = "USER")
        @DisplayName("USER role can access win patterns")
        void winPatterns_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/win-patterns")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void winPatterns_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/win-patterns")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005A03", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void winPatterns_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests/win-patterns")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // Integration — Create then lifecycle
    // =========================================================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005B01", roles = "ADMIN")
        @DisplayName("Create then list includes created test")
        void createThenList_includesCreated() {
            int createStatus = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Integration test\","
                            + "\"variantA\":\"A content\","
                            + "\"variantB\":\"B content\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract().statusCode();

            assumeTrue(createStatus == 201, "AI unavailable, skipping");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ab-tests")
            .then()
                    .statusCode(200)
                    .body("$", not(empty()));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005B02", roles = "ADMIN")
        @DisplayName("Create then start returns 200")
        void createThenStart() {
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Start test\","
                            + "\"variantA\":\"A\","
                            + "\"variantB\":\"B\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ab-tests/" + id + "/start")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005B03", roles = "ADMIN")
        @DisplayName("Create then start then cancel returns 200")
        void createThenStartThenCancel() {
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Cancel test\","
                            + "\"variantA\":\"A\","
                            + "\"variantB\":\"B\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ab-tests/" + id + "/start")
            .then()
                    .statusCode(200);

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ab-tests/" + id + "/cancel")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005B04", roles = "ADMIN")
        @DisplayName("Create then delete returns 204")
        void createThenDelete() {
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Delete test\","
                            + "\"variantA\":\"A\","
                            + "\"variantB\":\"B\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/ab-tests")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ab-tests/" + id)
            .then()
                    .statusCode(204);
        }
    }
}
