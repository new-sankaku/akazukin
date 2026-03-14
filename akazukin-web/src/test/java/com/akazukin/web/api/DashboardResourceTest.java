package com.akazukin.web.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("DashboardResource API Tests")
class DashboardResourceTest {

    // =========================================================================
    // GET /api/v1/dashboard/summary
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/dashboard/summary")
    class SummaryTests {

        @Test
        @DisplayName("Without auth - blocked")
        void getSummary_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001001", roles = "ADMIN")
        @DisplayName("Success - returns summary with all zero counts for new user")
        void getSummary_success_allZeros() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .statusCode(200)
                    .body("totalPosts", equalTo(0))
                    .body("publishedPosts", equalTo(0))
                    .body("failedPosts", equalTo(0))
                    .body("scheduledPosts", equalTo(0))
                    .body("connectedAccounts", equalTo(0))
                    .body("postCountByPlatform", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001002", roles = "ADMIN")
        @DisplayName("Summary has postCountByPlatform field")
        void getSummary_hasPostCountByPlatform() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .statusCode(200)
                    .body("postCountByPlatform", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001003", roles = "USER")
        @DisplayName("USER role can access summary (ADMIN or USER allowed)")
        void getSummary_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001004", roles = "ADMIN")
        @DisplayName("Different users see independent summaries")
        void getSummary_differentUser_independentData() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .statusCode(200)
                    .body("totalPosts", equalTo(0))
                    .body("connectedAccounts", equalTo(0));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001005", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed for dashboard")
        void getSummary_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/dashboard/analytics
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/dashboard/analytics")
    class AnalyticsTests {

        @Test
        @DisplayName("Without auth - blocked")
        void getAnalytics_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001101", roles = "ADMIN")
        @DisplayName("Success - returns analytics with zero counts for new user")
        void getAnalytics_success() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .statusCode(200)
                    .body("totalPosts", equalTo(0))
                    .body("publishedPosts", equalTo(0))
                    .body("connectedAccounts", equalTo(0));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001102", roles = "ADMIN")
        @DisplayName("Analytics includes failedPosts and scheduledPosts fields")
        void getAnalytics_allFields() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .statusCode(200)
                    .body("failedPosts", equalTo(0))
                    .body("scheduledPosts", equalTo(0))
                    .body("postCountByPlatform", notNullValue())
                    .body("accountStats", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001103", roles = "ADMIN")
        @DisplayName("New user analytics has empty accountStats list")
        void getAnalytics_emptyAccountStats() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .statusCode(200)
                    .body("accountStats", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001104", roles = "USER")
        @DisplayName("USER role can access analytics")
        void getAnalytics_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .statusCode(200);
        }
    }

    // =========================================================================
    // GET /api/v1/dashboard/timeline
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/dashboard/timeline")
    class TimelineTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001201", roles = "ADMIN")
        @DisplayName("Success - returns empty list for new user")
        void getTimeline_success_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001202", roles = "ADMIN")
        @DisplayName("With limit param returns 200")
        void getTimeline_withLimit() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", 5)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001203", roles = "ADMIN")
        @DisplayName("Default limit (no param) returns 200")
        void getTimeline_defaultLimit() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001204", roles = "ADMIN")
        @DisplayName("Limit 0 uses default (20) - returns 200")
        void getTimeline_limitZero() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", 0)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001205", roles = "ADMIN")
        @DisplayName("Limit > 100 is capped - returns 200")
        void getTimeline_limitOver100() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", 200)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001206", roles = "ADMIN")
        @DisplayName("Negative limit uses default (20) - returns 200")
        void getTimeline_negativeLimit() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", -5)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001207", roles = "ADMIN")
        @DisplayName("Limit 1 returns 200")
        void getTimeline_limitOne() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", 1)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001208", roles = "ADMIN")
        @DisplayName("Limit 100 (max) returns 200")
        void getTimeline_limitMax() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("limit", 100)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTimeline_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000001209", roles = "USER")
        @DisplayName("USER role can access timeline")
        void getTimeline_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .statusCode(200);
        }
    }
}
