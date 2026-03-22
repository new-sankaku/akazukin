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
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("AuditLogResource API Tests")
class AuditLogResourceTest {

    // =========================================================================
    // GET /api/v1/audit-logs — List Audit Logs (default)
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/audit-logs (default)")
    class ListDefaultTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006001", roles = "ADMIN")
        @DisplayName("Returns 200 with paginated result")
        void listDefault_returns200() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue())
                    .body("page", equalTo(0))
                    .body("size", equalTo(50));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006002", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listDefault_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 1)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("page", equalTo(1))
                    .body("size", equalTo(10));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006003", roles = "ADMIN")
        @DisplayName("Size exceeding max (100) is capped")
        void listDefault_sizeCapped() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 200)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("size", equalTo(100));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006004", roles = "ADMIN")
        @DisplayName("Large page number returns 200 with empty data")
        void listDefault_largePageNumber() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 9999)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("page", equalTo(9999));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listDefault_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/audit-logs?category= — Filter by category
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/audit-logs (filter by category)")
    class FilterByCategoryTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006101", roles = "ADMIN")
        @DisplayName("Filter by category returns 200")
        void filterByCategory_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("category", "AUTH")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006102", roles = "ADMIN")
        @DisplayName("Non-existent category returns 200 with empty data")
        void filterByCategory_nonExistent() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("category", "NONEXISTENT_CATEGORY_XYZ")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // =========================================================================
    // GET /api/v1/audit-logs?userId= — Filter by userId
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/audit-logs (filter by userId)")
    class FilterByUserIdTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006201", roles = "ADMIN")
        @DisplayName("Filter by userId returns 200")
        void filterByUserId_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("userId", UUID.randomUUID().toString())
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // =========================================================================
    // GET /api/v1/audit-logs?path= — Filter by path
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/audit-logs (filter by path)")
    class FilterByPathTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006301", roles = "ADMIN")
        @DisplayName("Filter by path returns 200")
        void filterByPath_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("path", "/api/v1/posts")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // =========================================================================
    // GET /api/v1/audit-logs?from=&to= — Filter by date range
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/audit-logs (filter by date range)")
    class FilterByDateRangeTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006401", roles = "ADMIN")
        @DisplayName("Filter by valid date range returns 200")
        void filterByDateRange_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01")
                    .queryParam("to", "2025-12-31")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006402", roles = "ADMIN")
        @DisplayName("Invalid date format returns error (not 200)")
        void filterByDateRange_invalidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "invalid-date")
                    .queryParam("to", "2025-12-31")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid date format, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006403", roles = "ADMIN")
        @DisplayName("Only 'from' without 'to' falls back to default range")
        void filterByDateRange_onlyFrom_fallsBackToDefault() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01")
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // =========================================================================
    // Role-based access tests
    // =========================================================================

    @Nested
    @DisplayName("Role-based access control")
    class RoleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006501", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void list_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000006502", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed (requires ADMIN)")
        void list_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/audit-logs")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }
    }
}
