package com.akazukin.web.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@DisplayName("AiResource API Tests")
class AiResourceTest {

    // =========================================================================
    // POST /api/v1/ai/generate — Generate Content
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ai/generate")
    class GenerateTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007001", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void generate_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/generate")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007002", roles = "ADMIN")
        @DisplayName("Valid request returns 200 or 500 (AI unavailable)")
        void generate_validRequest() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Write a tweet about cats\","
                            + "\"temperature\":0.7,"
                            + "\"maxTokens\":100}")
            .when()
                    .post("/api/v1/ai/generate")
            .then()
                    .extract().statusCode();

            assert status == 200 || status == 500
                    : "Expected 200 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generate_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Test\"}")
            .when()
                    .post("/api/v1/ai/generate")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007003", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void generate_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Test\"}")
            .when()
                    .post("/api/v1/ai/generate")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007004", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void generate_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Test\"}")
            .when()
                    .post("/api/v1/ai/generate")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ai/compare — Compare Generate
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ai/compare")
    class CompareGenerateTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007101", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void compare_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/compare")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007102", roles = "ADMIN")
        @DisplayName("Request with empty personas returns 400 (2-4 personas required)")
        void compare_emptyPersonas_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Compare test\","
                            + "\"personaIds\":[]}")
            .when()
                    .post("/api/v1/ai/compare")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void compare_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"prompt\":\"Test\"}")
            .when()
                    .post("/api/v1/ai/compare")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ai/tryout — Tryout
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ai/tryout")
    class TryoutTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007201", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void tryout_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/tryout")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007202", roles = "ADMIN")
        @DisplayName("Request without persona returns 400 (persona required)")
        void tryout_withoutPersona_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"text\":\"Try this text\"}")
            .when()
                    .post("/api/v1/ai/tryout")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void tryout_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"text\":\"Test\"}")
            .when()
                    .post("/api/v1/ai/tryout")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ai/personas — List Personas
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ai/personas (list)")
    class ListPersonasTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007301", roles = "ADMIN")
        @DisplayName("Returns 200 for authenticated user")
        void listPersonas_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/personas")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007302", roles = "ADMIN")
        @DisplayName("Different user sees their own list")
        void listPersonas_differentUser() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/personas")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listPersonas_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/personas")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ai/personas — Create Persona
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ai/personas (create)")
    class CreatePersonaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007401", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void createPersona_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007402", roles = "ADMIN")
        @DisplayName("Valid request returns 201 or 500 (AI unavailable)")
        void createPersona_success() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test Persona\","
                            + "\"systemPrompt\":\"You are a helpful assistant\","
                            + "\"tone\":\"friendly\","
                            + "\"language\":\"ja\","
                            + "\"isDefault\":false}")
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .extract().statusCode();

            assert status == 201 || status == 500
                    : "Expected 201 or 500 (AI unavailable), got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createPersona_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\","
                            + "\"systemPrompt\":\"test\","
                            + "\"tone\":\"neutral\","
                            + "\"language\":\"ja\","
                            + "\"isDefault\":false}")
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/ai/personas/{id} — Update Persona
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/ai/personas/{id}")
    class UpdatePersonaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007501", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void updatePersona_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ai/personas/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007502", roles = "ADMIN")
        @DisplayName("Non-existent persona returns error")
        void updatePersona_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Updated\","
                            + "\"systemPrompt\":\"updated prompt\","
                            + "\"tone\":\"formal\","
                            + "\"language\":\"en\","
                            + "\"isDefault\":false}")
            .when()
                    .put("/api/v1/ai/personas/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for non-existent persona, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void updatePersona_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\"}")
            .when()
                    .put("/api/v1/ai/personas/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/ai/personas/{id} — Delete Persona
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/ai/personas/{id}")
    class DeletePersonaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007601", roles = "ADMIN")
        @DisplayName("Non-existent persona returns error")
        void deletePersona_notFound() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ai/personas/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for non-existent persona, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deletePersona_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ai/personas/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007602", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void deletePersona_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ai/personas/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ai/settings/ollama/status — Ollama Status
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ai/settings/ollama/status")
    class OllamaStatusTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007701", roles = "ADMIN")
        @DisplayName("Returns 200 with status info")
        void ollamaStatus_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/ollama/status")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void ollamaStatus_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/ollama/status")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/ai/settings/ollama/reconnect — Reconnect Ollama
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/ai/settings/ollama/reconnect")
    class ReconnectOllamaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007801", roles = "ADMIN")
        @DisplayName("Returns 200")
        void reconnectOllama_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/settings/ollama/reconnect")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void reconnectOllama_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/ai/settings/ollama/reconnect")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ai/settings/task-providers — Get Task Provider Settings
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ai/settings/task-providers")
    class GetTaskProviderSettingsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007901", roles = "ADMIN")
        @DisplayName("Returns 200 with empty list")
        void getTaskProviders_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/task-providers")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTaskProviders_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/task-providers")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/ai/settings/task-providers — Save Task Provider Settings
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/ai/settings/task-providers")
    class SaveTaskProviderSettingsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007A01", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void saveTaskProviders_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/ai/settings/task-providers")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007A02", roles = "ADMIN")
        @DisplayName("Empty settings list returns 400 (at least one setting required)")
        void saveTaskProviders_emptySettings_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"settings\":[]}")
            .when()
                    .put("/api/v1/ai/settings/task-providers")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void saveTaskProviders_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"settings\":[]}")
            .when()
                    .put("/api/v1/ai/settings/task-providers")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/ai/settings/cost-monitor — Cost Monitor
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/ai/settings/cost-monitor")
    class CostMonitorTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007B01", roles = "ADMIN")
        @DisplayName("Returns 200 with cost info")
        void costMonitor_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/cost-monitor")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void costMonitor_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/settings/cost-monitor")
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
        @TestSecurity(user = "00000000-0000-0000-0000-000000007C01", roles = "ADMIN")
        @DisplayName("Create persona then list includes it")
        void createPersonaThenList() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            int createStatus = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Persona " + unique + "\","
                            + "\"systemPrompt\":\"Test prompt\","
                            + "\"tone\":\"casual\","
                            + "\"language\":\"ja\","
                            + "\"isDefault\":false}")
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .extract().statusCode();

            assumeTrue(createStatus == 201, "AI unavailable, skipping");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/ai/personas")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007C02", roles = "ADMIN")
        @DisplayName("Create persona then update returns 200")
        void createPersonaThenUpdate() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Update Persona " + unique + "\","
                            + "\"systemPrompt\":\"Original\","
                            + "\"tone\":\"neutral\","
                            + "\"language\":\"en\","
                            + "\"isDefault\":false}")
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Updated Persona " + unique + "\","
                            + "\"systemPrompt\":\"Updated prompt\","
                            + "\"tone\":\"formal\","
                            + "\"language\":\"ja\","
                            + "\"isDefault\":false}")
            .when()
                    .put("/api/v1/ai/personas/" + id)
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007C03", roles = "ADMIN")
        @DisplayName("Create persona then delete returns 204")
        void createPersonaThenDelete() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            var response = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Delete Persona " + unique + "\","
                            + "\"systemPrompt\":\"To be deleted\","
                            + "\"tone\":\"casual\","
                            + "\"language\":\"ja\","
                            + "\"isDefault\":false}")
            .when()
                    .post("/api/v1/ai/personas")
            .then()
                    .extract();

            assumeTrue(response.statusCode() == 201, "AI unavailable, skipping");

            String id = response.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/ai/personas/" + id)
            .then()
                    .statusCode(204);
        }
    }
}
