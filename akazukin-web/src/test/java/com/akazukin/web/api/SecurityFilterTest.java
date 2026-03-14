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

@QuarkusTest
@DisplayName("SecurityFilter Tests")
class SecurityFilterTest {

    // =========================================================================
    // Public path access tests
    // =========================================================================

    @Nested
    @DisplayName("Public paths - accessible without auth")
    class PublicPathTests {

        @Test
        @DisplayName("Auth login endpoint is accessible without auth")
        void authLoginPath_noAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"sectest_login\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(not(equalTo(401)));
        }

        @Test
        @DisplayName("Auth register endpoint is accessible without auth")
        void authRegisterPath_noAuth_allowed() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"sectest_reg_" + unique + "\","
                            + "\"email\":\"sectest_reg_" + unique + "@example.com\","
                            + "\"password\":\"pw123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(201);
        }

        @Test
        @DisplayName("Auth refresh endpoint is accessible without auth")
        void authRefreshPath_noAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"some.invalid.token\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(401); // Business error, not filter block
        }

        @Test
        @DisplayName("Quarkus dev paths (/q/) should be public")
        void quarkusDevPath_noAuth_allowed() {
            // /q/health would be public if health extension is loaded
            // We just verify it does not return 401
            int status = given()
            .when()
                    .get("/q/")
            .then()
                    .extract().statusCode();

            assert status != 401 : "Quarkus dev path should not return 401, got " + status;
        }
    }

    // =========================================================================
    // Protected path access tests (without auth)
    // =========================================================================

    @Nested
    @DisplayName("Protected paths - blocked without auth")
    class ProtectedPathBlockedTests {

        @Test
        @DisplayName("GET /api/v1/posts - blocked without auth")
        void postsPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("POST /api/v1/posts - blocked without auth")
        void postsPostPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"test\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201 && status != 202
                    : "Expected non-success for unauthenticated POST, got " + status;
        }

        @Test
        @DisplayName("PUT /api/v1/posts/{id} - blocked without auth")
        void postsPutPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"content\":\"test\",\"targetPlatforms\":[\"TWITTER\"]}")
            .when()
                    .put("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated PUT, got " + status;
        }

        @Test
        @DisplayName("DELETE /api/v1/posts/{id} - blocked without auth")
        void postsDeletePath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/posts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @DisplayName("GET /api/v1/dashboard/summary - blocked without auth")
        void dashboardSummaryPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("GET /api/v1/dashboard/analytics - blocked without auth")
        void dashboardAnalyticsPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/analytics")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("GET /api/v1/dashboard/timeline - blocked without auth")
        void dashboardTimelinePath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/timeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("GET /api/v1/accounts - blocked without auth")
        void accountsPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("GET /api/v1/teams - blocked without auth")
        void teamsPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @DisplayName("POST /api/v1/teams - blocked without auth")
        void teamsPostPath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"test team\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated POST, got " + status;
        }

        @Test
        @DisplayName("DELETE /api/v1/teams/{id} - blocked without auth")
        void teamsDeletePath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @DisplayName("DELETE /api/v1/accounts/{id} - blocked without auth")
        void accountsDeletePath_noAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/accounts/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }
    }

    // =========================================================================
    // Protected path access tests (with valid auth)
    // =========================================================================

    @Nested
    @DisplayName("Protected paths - accessible with valid auth")
    class ProtectedPathAllowedTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000003001", roles = "ADMIN")
        @DisplayName("GET /api/v1/posts - allowed with valid ADMIN auth")
        void postsPath_validAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/posts")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000003002", roles = "ADMIN")
        @DisplayName("GET /api/v1/dashboard/summary - allowed with valid auth")
        void dashboardPath_validAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/dashboard/summary")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000003003", roles = "ADMIN")
        @DisplayName("GET /api/v1/accounts - allowed with valid auth")
        void accountsPath_validAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000003004", roles = "ADMIN")
        @DisplayName("GET /api/v1/teams - allowed with valid auth")
        void teamsPath_validAuth_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .statusCode(200);
        }
    }

    // =========================================================================
    // Invalid token tests
    // =========================================================================

    @Nested
    @DisplayName("Invalid/expired token tests")
    class InvalidTokenTests {

        @Test
        @DisplayName("Invalid cookie token - blocked")
        void protectedPath_invalidCookieToken_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .cookie("QuarkusUser", "invalid.token.value")
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid token, got " + status;
        }

        @Test
        @DisplayName("Invalid Bearer token - blocked")
        void protectedPath_invalidBearerToken_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer invalid.token.value")
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid bearer token, got " + status;
        }

        @Test
        @DisplayName("Empty Bearer token - blocked")
        void protectedPath_emptyBearerToken_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer ")
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for empty bearer token, got " + status;
        }

        @Test
        @DisplayName("No Bearer prefix - blocked")
        void protectedPath_noBearerPrefix_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "some.token.value")
            .when()
                    .get("/api/v1/posts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for missing Bearer prefix, got " + status;
        }
    }
}
