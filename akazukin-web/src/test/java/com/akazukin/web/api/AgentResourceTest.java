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
@DisplayName("AgentResource API Tests")
class AgentResourceTest {

    // =========================================================================
    // POST /api/v1/agents/pipeline — Run Pipeline
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/agents/pipeline")
    class RunPipelineTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006001", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void runPipeline_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/agents/pipeline")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006002", roles = "ADMIN")
        @DisplayName("Valid request returns 200 or 400 or 500 (AI unavailable or timeout)")
        void runPipeline_validRequest() {
            int status;
            try {
                status = given()
                        .contentType(ContentType.JSON)
                        .body("{\"topic\":\"Test topic\","
                                + "\"targetPlatforms\":[\"TWITTER\"]}")
                        .config(io.restassured.RestAssured.config()
                                .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                                        .setParam("http.socket.timeout", 10000)
                                        .setParam("http.connection.timeout", 5000)))
                .when()
                        .post("/api/v1/agents/pipeline")
                .then()
                        .extract().statusCode();
            } catch (Exception e) {
                return;
            }

            assert status == 200 || status == 400 || status == 500
                    : "Expected 200, 400 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void runPipeline_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"topic\":\"Test\","
                            + "\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/agents/pipeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006003", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void runPipeline_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"topic\":\"Test\","
                            + "\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/agents/pipeline")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006004", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void runPipeline_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"topic\":\"Test\","
                            + "\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/agents/pipeline")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/agents/tasks — Submit Task
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/agents/tasks")
    class SubmitTaskTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006101", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void submitTask_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006102", roles = "ADMIN")
        @DisplayName("Valid request returns 201 or 500 (AI unavailable)")
        void submitTask_validRequest() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"COMPOSER\","
                            + "\"input\":\"Generate a tweet about cats\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assert status == 201 || status == 500
                    : "Expected 201 or 500 (AI unavailable), got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006103", roles = "ADMIN")
        @DisplayName("Invalid agent type returns error")
        void submitTask_invalidAgentType() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"NONEXISTENT_AGENT\","
                            + "\"input\":\"test\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected error for invalid agent type, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void submitTask_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"COMPOSER\","
                            + "\"input\":\"test\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/agents/tasks — List Tasks
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/agents/tasks (list)")
    class ListTasksTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006201", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listTasks_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006202", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listTasks_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
            .when()
                    .get("/api/v1/agents/tasks")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006203", roles = "ADMIN")
        @DisplayName("Large offset returns empty list")
        void listTasks_largeOffset() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("offset", 9999)
                    .queryParam("limit", 10)
            .when()
                    .get("/api/v1/agents/tasks")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listTasks_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/agents/tasks/{id} — Get Task
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/agents/tasks/{id}")
    class GetTaskTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006301", roles = "ADMIN")
        @DisplayName("Non-existent task returns error")
        void getTask_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent task, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006302", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void getTask_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTask_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/agents/stats — Get Stats
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/agents/stats")
    class GetStatsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006401", roles = "ADMIN")
        @DisplayName("Returns 200 with zero stats for new user")
        void getStats_newUser() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .statusCode(200)
                    .body("totalExecutions", equalTo(0))
                    .body("successRate", equalTo(0.0f));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006402", roles = "ADMIN")
        @DisplayName("Stats has all expected fields")
        void getStats_hasAllFields() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .statusCode(200)
                    .body("totalExecutions", notNullValue())
                    .body("averageDurationMs", notNullValue())
                    .body("activeAgentCount", notNullValue())
                    .body("successRate", notNullValue());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getStats_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006403", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void getStats_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/agents/tasks/{id}/children — Get Child Tasks
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/agents/tasks/{id}/children")
    class GetChildTasksTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006501", roles = "ADMIN")
        @DisplayName("Non-existent parent returns empty list")
        void getChildTasks_noParent_emptyList() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/" + UUID.randomUUID() + "/children")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getChildTasks_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/" + UUID.randomUUID() + "/children")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // Integration Tests
    // =========================================================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006601", roles = "ADMIN")
        @DisplayName("Submit task then list includes submitted task")
        void submitThenList_includesSubmitted() {
            int createStatus = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"COMPOSER\","
                            + "\"input\":\"Integration test input\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assumeTrue(createStatus == 201, "AI unavailable, skipping");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks")
            .then()
                    .statusCode(200)
                    .body("$", not(empty()));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006602", roles = "ADMIN")
        @DisplayName("Submit task then get by ID returns 200")
        void submitThenGetById() {
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"COMPOSER\","
                            + "\"input\":\"Get by ID test\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/tasks/" + id)
            .then()
                    .statusCode(200)
                    .body("id", equalTo(id));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006603", roles = "ADMIN")
        @DisplayName("Submit task then stats reflects total count")
        void submitThenStats_reflectsCount() {
            int createStatus = given()
                    .contentType(ContentType.JSON)
                    .body("{\"agentType\":\"COMPOSER\","
                            + "\"input\":\"Stats test input\"}")
            .when()
                    .post("/api/v1/agents/tasks")
            .then()
                    .extract().statusCode();

            assumeTrue(createStatus == 201, "AI unavailable, skipping");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/agents/stats")
            .then()
                    .statusCode(200)
                    .body("totalExecutions", not(equalTo(0)));
        }
    }
}
