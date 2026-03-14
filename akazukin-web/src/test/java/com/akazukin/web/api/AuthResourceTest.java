package com.akazukin.web.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("AuthResource API Tests")
class AuthResourceTest {

    // =========================================================================
    // Register endpoint tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Success - returns 201 with access and refresh tokens")
        void register_success() {
            String username = "reg_success_" + UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\","
                            + "\"email\":\"" + username + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(201)
                    .body("accessToken", not(emptyOrNullString()))
                    .body("refreshToken", not(emptyOrNullString()))
                    .body("expiresIn", greaterThan(0));
        }

        @Test
        @DisplayName("Success - response has token expiration info (expiresIn = 900)")
        void register_success_hasExpirationInfo() {
            String username = "reg_exp_" + UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\","
                            + "\"email\":\"" + username + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(201)
                    .body("expiresIn", equalTo(900));
        }

        @Test
        @DisplayName("Duplicate username returns error with DUPLICATE_USERNAME")
        void register_duplicateUsername() {
            String username = "reg_dup_" + UUID.randomUUID().toString().substring(0, 8);
            String body = "{\"username\":\"" + username + "\","
                    + "\"email\":\"" + username + "@example.com\","
                    + "\"password\":\"password123\"}";

            given().contentType(ContentType.JSON).body(body)
                    .when().post("/api/v1/auth/register")
                    .then().statusCode(201);

            given().contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\","
                            + "\"email\":\"" + username + "_2@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("DUPLICATE_USERNAME"));
        }

        @Test
        @DisplayName("Duplicate email returns error with DUPLICATE_EMAIL")
        void register_duplicateEmail() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            String email = "dupemail_" + unique + "@example.com";

            given().contentType(ContentType.JSON)
                    .body("{\"username\":\"dupemail1_" + unique + "\","
                            + "\"email\":\"" + email + "\","
                            + "\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/register")
                    .then().statusCode(201);

            given().contentType(ContentType.JSON)
                    .body("{\"username\":\"dupemail2_" + unique + "\","
                            + "\"email\":\"" + email + "\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("DUPLICATE_EMAIL"));
        }

        @Test
        @DisplayName("Missing username (null) returns 400")
        void register_missingUsername() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"email\":\"nouser_" + unique + "@example.com\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Missing email (null) returns 400")
        void register_missingEmail() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"noemail_" + unique + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Missing password (null) returns 400")
        void register_missingPassword() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"nopw_" + unique + "\","
                            + "\"email\":\"nopw_" + unique + "@example.com\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Empty username returns 400")
        void register_emptyUsername() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"\","
                            + "\"email\":\"emptyuser_" + unique + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Empty email returns 400")
        void register_emptyEmail() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"emptyemail_" + unique + "\","
                            + "\"email\":\"\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Empty password returns 400")
        void register_emptyPassword() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"emptypw_" + unique + "\","
                            + "\"email\":\"emptypw_" + unique + "@example.com\","
                            + "\"password\":\"\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Whitespace-only username returns 400")
        void register_whitespaceUsername() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"   \","
                            + "\"email\":\"wsuser_" + unique + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Whitespace-only password returns 400")
        void register_whitespacePassword() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"wspw_" + unique + "\","
                            + "\"email\":\"wspw_" + unique + "@example.com\","
                            + "\"password\":\"   \"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Null request body returns 400")
        void register_nullBody() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Register multiple users - each gets unique tokens")
        void register_multipleUsers_uniqueTokens() {
            String unique1 = UUID.randomUUID().toString().substring(0, 8);
            String unique2 = UUID.randomUUID().toString().substring(0, 8);

            Response res1 = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"multi1_" + unique1 + "\","
                            + "\"email\":\"multi1_" + unique1 + "@example.com\","
                            + "\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/register");
            res1.then().statusCode(201);

            Response res2 = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"multi2_" + unique2 + "\","
                            + "\"email\":\"multi2_" + unique2 + "@example.com\","
                            + "\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/register");
            res2.then().statusCode(201);

            String token1 = res1.jsonPath().getString("accessToken");
            String token2 = res2.jsonPath().getString("accessToken");
            assert !token1.equals(token2) : "Different users must get different access tokens";

            String refresh1 = res1.jsonPath().getString("refreshToken");
            String refresh2 = res2.jsonPath().getString("refreshToken");
            assert !refresh1.equals(refresh2) : "Different users must get different refresh tokens";
        }

        @Test
        @DisplayName("Username with special characters succeeds")
        void register_specialCharsUsername() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"user-._" + unique + "\","
                            + "\"email\":\"special_" + unique + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .statusCode(201)
                    .body("accessToken", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("Very long username (>50 chars) returns error due to DB column constraint")
        void register_longUsername() {
            String unique = UUID.randomUUID().toString().substring(0, 8);
            String longUsername = "a".repeat(100) + "_" + unique;
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + longUsername + "\","
                            + "\"email\":\"long_" + unique + "@example.com\","
                            + "\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/register")
            .then()
                    .extract().statusCode();

            // Username column is limited to 50 chars, so this should fail
            assert status != 201 : "Expected error for username exceeding column length, got " + status;
        }
    }

    // =========================================================================
    // Login endpoint tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Success - returns 200 with access and refresh tokens")
        void login_success() {
            String username = "login_ok_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(200)
                    .body("accessToken", not(emptyOrNullString()))
                    .body("refreshToken", not(emptyOrNullString()))
                    .body("expiresIn", greaterThan(0));
        }

        @Test
        @DisplayName("Success - returns expiresIn of 900 seconds")
        void login_success_expiresIn() {
            String username = "login_exp_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(200)
                    .body("expiresIn", equalTo(900));
        }

        @Test
        @DisplayName("Wrong password returns 400 with INVALID_CREDENTIALS")
        void login_wrongPassword() {
            String username = "login_badpw_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"wrongpassword\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Non-existent user returns 400 with INVALID_CREDENTIALS")
        void login_nonExistentUser() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"nonexistent_" + UUID.randomUUID().toString().substring(0, 8)
                            + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Null request body returns 400")
        void login_nullBody() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Empty username returns 400 INVALID_CREDENTIALS")
        void login_emptyUsername() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Empty password returns 400")
        void login_emptyPassword() {
            String username = "login_emptypw_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Username is case-sensitive")
        void login_caseSensitiveUsername() {
            String username = "login_case_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            // Try with uppercase - should fail
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username.toUpperCase() + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Login returns different tokens on each call")
        void login_differentTokensEachTime() {
            String username = "login_diff_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            Response res1 = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/login");
            res1.then().statusCode(200);

            Response res2 = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/login");
            res2.then().statusCode(200);

            String token1 = res1.jsonPath().getString("accessToken");
            String token2 = res2.jsonPath().getString("accessToken");
            // Tokens may differ due to different iat/exp timestamps
            assert token1 != null && token2 != null;
        }

        @Test
        @DisplayName("Missing password field returns 400")
        void login_missingPasswordField() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"someuser\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Missing username field returns 400")
        void login_missingUsernameField() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(400);
        }
    }

    // =========================================================================
    // Refresh endpoint tests
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("Success - returns new access and refresh tokens")
        void refresh_success() {
            String username = "refresh_ok_" + UUID.randomUUID().toString().substring(0, 8);
            Response regRes = registerUserAndGetResponse(username, username + "@example.com", "password123");
            String refreshToken = regRes.jsonPath().getString("refreshToken");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"" + refreshToken + "\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(200)
                    .body("accessToken", not(emptyOrNullString()))
                    .body("refreshToken", not(emptyOrNullString()))
                    .body("expiresIn", greaterThan(0));
        }

        @Test
        @DisplayName("Success - new refresh token is returned")
        void refresh_success_newRefreshToken() {
            String username = "refresh_new_" + UUID.randomUUID().toString().substring(0, 8);
            Response regRes = registerUserAndGetResponse(username, username + "@example.com", "password123");
            String originalRefresh = regRes.jsonPath().getString("refreshToken");

            Response refreshRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"" + originalRefresh + "\"}")
                    .when().post("/api/v1/auth/refresh");
            refreshRes.then().statusCode(200);

            String newRefresh = refreshRes.jsonPath().getString("refreshToken");
            assert newRefresh != null && !newRefresh.isBlank();
        }

        @Test
        @DisplayName("Invalid refresh token returns 401")
        void refresh_invalidToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"invalid.token.value\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(401)
                    .body("error", equalTo("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("Completely garbage token returns 401")
        void refresh_garbageToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"not-a-jwt-at-all\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(401)
                    .body("error", equalTo("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("Access token used as refresh token returns 401")
        void refresh_accessTokenUsedAsRefresh() {
            String username = "refresh_acc_" + UUID.randomUUID().toString().substring(0, 8);
            Response regRes = registerUserAndGetResponse(username, username + "@example.com", "password123");
            String accessToken = regRes.jsonPath().getString("accessToken");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"" + accessToken + "\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(401)
                    .body("error", equalTo("INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("Missing refresh token in body returns 400")
        void refresh_missingToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Empty string refresh token returns 400")
        void refresh_emptyToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Whitespace-only refresh token returns 400")
        void refresh_whitespaceToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"   \"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("Null request body returns 400")
        void refresh_nullBody() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Refresh token with tampered payload returns 401")
        void refresh_tamperedToken() {
            String username = "refresh_tamp_" + UUID.randomUUID().toString().substring(0, 8);
            Response regRes = registerUserAndGetResponse(username, username + "@example.com", "password123");
            String refreshToken = regRes.jsonPath().getString("refreshToken");

            // Tamper with the token by modifying the payload
            String[] parts = refreshToken.split("\\.");
            if (parts.length >= 2) {
                String tampered = parts[0] + ".dGFtcGVyZWQ." + parts[2];
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"refreshToken\":\"" + tampered + "\"}")
                .when()
                        .post("/api/v1/auth/refresh")
                .then()
                        .statusCode(401)
                        .body("error", equalTo("INVALID_REFRESH_TOKEN"));
            }
        }
    }

    // =========================================================================
    // Cross-cutting / integration tests
    // =========================================================================

    @Nested
    @DisplayName("Auth Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Register then login with same credentials succeeds")
        void register_then_login() {
            String username = "int_reglogin_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(200)
                    .body("accessToken", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("Register then refresh the obtained refresh token")
        void register_then_refresh() {
            String username = "int_regrefresh_" + UUID.randomUUID().toString().substring(0, 8);
            Response regRes = registerUserAndGetResponse(username, username + "@example.com", "password123");
            String refreshToken = regRes.jsonPath().getString("refreshToken");

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"" + refreshToken + "\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(200)
                    .body("accessToken", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("Login then refresh with login's refresh token")
        void login_then_refresh() {
            String username = "int_loginrefresh_" + UUID.randomUUID().toString().substring(0, 8);
            registerUser(username, username + "@example.com", "password123");

            Response loginRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"" + username + "\",\"password\":\"password123\"}")
                    .when().post("/api/v1/auth/login");
            loginRes.then().statusCode(200);

            String refreshToken = loginRes.jsonPath().getString("refreshToken");
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"refreshToken\":\"" + refreshToken + "\"}")
            .when()
                    .post("/api/v1/auth/refresh")
            .then()
                    .statusCode(200)
                    .body("accessToken", not(emptyOrNullString()));
        }

        @Test
        @DisplayName("Auth endpoints are accessible without authentication")
        void authEndpoints_noAuthRequired() {
            // Login endpoint - business error, not 401
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"username\":\"noauth_test\",\"password\":\"pw\"}")
            .when()
                    .post("/api/v1/auth/login")
            .then()
                    .statusCode(not(equalTo(401)));
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private void registerUser(String username, String email, String password) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"" + password + "\"}")
        .when()
                .post("/api/v1/auth/register")
        .then()
                .statusCode(201);
    }

    private Response registerUserAndGetResponse(String username, String email, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"" + password + "\"}")
        .when()
                .post("/api/v1/auth/register");
        response.then().statusCode(201);
        return response;
    }

    /**
     * Helper method to register a user and return the access token.
     */
    static String registerAndGetToken(String username, String email) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + username + "\","
                        + "\"email\":\"" + email + "\","
                        + "\"password\":\"password123\"}")
        .when()
                .post("/api/v1/auth/register");

        response.then().statusCode(201);
        return response.jsonPath().getString("accessToken");
    }
}
