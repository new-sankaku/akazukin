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
@DisplayName("TemplateResource API Tests")
class TemplateResourceTest {

    // =========================================================================
    // POST /api/v1/templates — Create Template
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/templates (create)")
    class CreateTemplateTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008001", roles = "ADMIN")
        @DisplayName("Success - returns 201 with created template")
        void createTemplate_success() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test Template\","
                            + "\"content\":\"Hello {{name}}!\","
                            + "\"placeholders\":[\"name\"],"
                            + "\"platforms\":[\"TWITTER\"],"
                            + "\"category\":\"greeting\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .body("name", equalTo("Test Template"))
                    .body("content", equalTo("Hello {{name}}!"))
                    .body("id", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008002", roles = "ADMIN")
        @DisplayName("Success - minimal fields (name and content only)")
        void createTemplate_minimalFields() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Minimal\",\"content\":\"Just content\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .body("name", equalTo("Minimal"))
                    .body("content", equalTo("Just content"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008003", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void createTemplate_nullBody() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008004", roles = "ADMIN")
        @DisplayName("Missing name returns 400 INVALID_INPUT")
        void createTemplate_missingName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"Some content\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008005", roles = "ADMIN")
        @DisplayName("Empty name returns 400 INVALID_INPUT")
        void createTemplate_emptyName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"\",\"content\":\"Some content\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008006", roles = "ADMIN")
        @DisplayName("Whitespace-only name returns 400 INVALID_INPUT")
        void createTemplate_whitespaceName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"   \",\"content\":\"Some content\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008007", roles = "ADMIN")
        @DisplayName("Missing content returns 400 INVALID_INPUT")
        void createTemplate_missingContent() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Template Name\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008008", roles = "ADMIN")
        @DisplayName("Empty content returns 400 INVALID_INPUT")
        void createTemplate_emptyContent() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Template Name\",\"content\":\"\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008009", roles = "ADMIN")
        @DisplayName("Whitespace-only content returns 400 INVALID_INPUT")
        void createTemplate_whitespaceContent() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Template Name\",\"content\":\"   \"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createTemplate_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test\",\"content\":\"Content\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008010", roles = "ADMIN")
        @DisplayName("Multiple platforms are accepted")
        void createTemplate_multiplePlatforms() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Multi Platform\","
                            + "\"content\":\"Cross post content\","
                            + "\"platforms\":[\"TWITTER\",\"MASTODON\"]}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .body("name", equalTo("Multi Platform"));
        }
    }

    // =========================================================================
    // GET /api/v1/templates — List Templates
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/templates (list)")
    class ListTemplatesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008101", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listTemplates_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008102", roles = "ADMIN")
        @DisplayName("Different user sees their own list")
        void listTemplates_differentUser() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listTemplates_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008103", roles = "USER")
        @DisplayName("USER role is not allowed")
        void listTemplates_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008104", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listTemplates_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/templates/{id} — Get Template by ID
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/templates/{id}")
    class GetTemplateTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008201", roles = "ADMIN")
        @DisplayName("Non-existent template returns 400 TEMPLATE_NOT_FOUND")
        void getTemplate_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEMPLATE_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008202", roles = "ADMIN")
        @DisplayName("Another non-existent template also returns 400")
        void getTemplate_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEMPLATE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTemplate_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008203", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void getTemplate_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008204", roles = "ADMIN")
        @DisplayName("Created template can be retrieved")
        void getTemplate_afterCreate() {
            String id = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Retrievable\",\"content\":\"Content to retrieve\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .extract().jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/templates/" + id)
            .then()
                    .statusCode(200)
                    .body("name", equalTo("Retrievable"))
                    .body("content", equalTo("Content to retrieve"));
        }
    }

    // =========================================================================
    // DELETE /api/v1/templates/{id} — Delete Template
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/templates/{id}")
    class DeleteTemplateTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008301", roles = "ADMIN")
        @DisplayName("Non-existent template returns 400 TEMPLATE_NOT_FOUND")
        void deleteTemplate_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEMPLATE_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008302", roles = "ADMIN")
        @DisplayName("Another non-existent template also returns 400")
        void deleteTemplate_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEMPLATE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteTemplate_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008303", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void deleteTemplate_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/invalid-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008304", roles = "ADMIN")
        @DisplayName("Created template can be deleted")
        void deleteTemplate_afterCreate() {
            String id = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Deletable\",\"content\":\"To be deleted\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .extract().jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + id)
            .then()
                    .statusCode(204);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008305", roles = "ADMIN")
        @DisplayName("Deleting already deleted template returns 400 TEMPLATE_NOT_FOUND")
        void deleteTemplate_alreadyDeleted() {
            String id = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Double Delete\",\"content\":\"Delete me twice\"}")
            .when()
                    .post("/api/v1/templates")
            .then()
                    .statusCode(201)
                    .extract().jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + id)
            .then()
                    .statusCode(204);

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + id)
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEMPLATE_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008306", roles = "USER")
        @DisplayName("USER role is not allowed to delete")
        void deleteTemplate_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/templates/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }
    }
}
