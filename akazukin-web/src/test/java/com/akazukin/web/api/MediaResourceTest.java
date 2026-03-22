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
@DisplayName("MediaResource API Tests")
class MediaResourceTest {

    // =========================================================================
    // GET /api/v1/media — List Media Assets
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/media (list)")
    class ListMediaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listMedia_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006002", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listMedia_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006003", roles = "ADMIN")
        @DisplayName("Large page number returns empty list")
        void listMedia_largePageNumber() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 9999)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006004", roles = "ADMIN")
        @DisplayName("Page size 1 returns 200")
        void listMedia_pageSize1() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006005", roles = "ADMIN")
        @DisplayName("Default pagination (no params) returns 200")
        void listMedia_defaultPagination() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listMedia_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006006", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void listMedia_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006007", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listMedia_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006008", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listMedia_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/media")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }
    }

    // =========================================================================
    // POST /api/v1/media — Upload Media (multipart)
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/media (upload)")
    class UploadMediaTests {

        @Test
        @DisplayName("Without auth - blocked")
        void uploadMedia_withoutAuth_blocked() {
            int status = given()
                    .multiPart("file", "test.txt", "hello".getBytes(), "text/plain")
            .when()
                    .post("/api/v1/media")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006101", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void uploadMedia_userRole_forbidden() {
            int status = given()
                    .multiPart("file", "test.txt", "hello".getBytes(), "text/plain")
            .when()
                    .post("/api/v1/media")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/media/{id} — Delete Media Asset
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/media/{id}")
    class DeleteMediaTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006201", roles = "ADMIN")
        @DisplayName("Non-existent asset returns 400 ASSET_NOT_FOUND")
        void deleteMedia_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/media/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ASSET_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006202", roles = "ADMIN")
        @DisplayName("Another non-existent asset UUID also returns 400")
        void deleteMedia_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/media/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ASSET_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteMedia_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/media/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006203", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void deleteMedia_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/media/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006204", roles = "USER")
        @DisplayName("USER role is not allowed for delete (requires ADMIN)")
        void deleteMedia_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/media/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }
    }
}
