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
@DisplayName("AccountResource API Tests")
class AccountResourceTest {

    // =========================================================================
    // GET /api/v1/accounts — List Accounts
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/accounts (list)")
    class ListAccountsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listAccounts_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004002", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listAccounts_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listAccounts_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004003", roles = "USER")
        @DisplayName("USER role can list accounts")
        void listAccounts_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004004", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listAccounts_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/accounts/{id} — Delete Account
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/accounts/{id}")
    class DeleteAccountTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004101", roles = "ADMIN")
        @DisplayName("Non-existent account returns 404")
        void deleteAccount_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/accounts/" + UUID.randomUUID())
            .then()
                    .statusCode(404);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004102", roles = "ADMIN")
        @DisplayName("Another non-existent account UUID also returns 404")
        void deleteAccount_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/accounts/" + UUID.randomUUID())
            .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteAccount_withoutAuth_blocked() {
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
    // GET /api/v1/accounts/{platform}/auth — Get Authorization URL
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/accounts/{platform}/auth")
    class GetAuthorizationUrlTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004201", roles = "ADMIN")
        @DisplayName("Invalid platform returns 400 INVALID_PLATFORM")
        void getAuthUrl_invalidPlatform() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
            .when()
                    .get("/api/v1/accounts/NONEXISTENT/auth")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_PLATFORM"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004202", roles = "ADMIN")
        @DisplayName("Missing callbackUrl returns 400 INVALID_REQUEST")
        void getAuthUrl_missingCallbackUrl() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/accounts/TWITTER/auth")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004203", roles = "ADMIN")
        @DisplayName("Empty callbackUrl returns 400 INVALID_REQUEST")
        void getAuthUrl_emptyCallbackUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "")
            .when()
                    .get("/api/v1/accounts/TWITTER/auth")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004204", roles = "ADMIN")
        @DisplayName("Whitespace-only callbackUrl returns 400 INVALID_REQUEST")
        void getAuthUrl_whitespaceCallbackUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "   ")
            .when()
                    .get("/api/v1/accounts/TWITTER/auth")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004205", roles = "ADMIN")
        @DisplayName("Valid platform with registered adapter returns 200 with auth URL")
        void getAuthUrl_validPlatformWithAdapter() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
            .when()
                    .get("/api/v1/accounts/TWITTER/auth")
            .then()
                    .statusCode(200)
                    .body("authorizationUrl", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004206", roles = "ADMIN")
        @DisplayName("Case-insensitive platform name works")
        void getAuthUrl_caseInsensitivePlatform() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
            .when()
                    .get("/api/v1/accounts/twitter/auth")
            .then()
                    .statusCode(200)
                    .body("authorizationUrl", notNullValue());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004207", roles = "ADMIN")
        @DisplayName("Mixed case platform name works")
        void getAuthUrl_mixedCasePlatform() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
            .when()
                    .get("/api/v1/accounts/Twitter/auth")
            .then()
                    .statusCode(200)
                    .body("authorizationUrl", notNullValue());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getAuthUrl_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
            .when()
                    .get("/api/v1/accounts/TWITTER/auth")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/accounts/{platform}/callback — OAuth Callback
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/accounts/{platform}/callback")
    class OAuthCallbackTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004301", roles = "ADMIN")
        @DisplayName("Invalid platform returns 400 INVALID_PLATFORM")
        void callback_invalidPlatform() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
                    .body("{\"code\":\"test-code\",\"state\":\"test-state\"}")
            .when()
                    .post("/api/v1/accounts/NONEXISTENT/callback")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_PLATFORM"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004302", roles = "ADMIN")
        @DisplayName("Missing callbackUrl returns 400 INVALID_REQUEST")
        void callback_missingCallbackUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"code\":\"test-code\",\"state\":\"test-state\"}")
            .when()
                    .post("/api/v1/accounts/TWITTER/callback")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004303", roles = "ADMIN")
        @DisplayName("Empty callbackUrl returns 400 INVALID_REQUEST")
        void callback_emptyCallbackUrl() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "")
                    .body("{\"code\":\"test-code\",\"state\":\"test-state\"}")
            .when()
                    .post("/api/v1/accounts/TWITTER/callback")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000004304", roles = "ADMIN")
        @DisplayName("Valid platform callback with invalid code returns error")
        void callback_invalidCode() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
                    .body("{\"code\":\"test-code\",\"state\":\"test-state\"}")
            .when()
                    .post("/api/v1/accounts/TWITTER/callback")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid OAuth code, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void callback_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("callbackUrl", "http://localhost:8080/callback")
                    .body("{\"code\":\"test-code\",\"state\":\"test-state\"}")
            .when()
                    .post("/api/v1/accounts/TWITTER/callback")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }
}
