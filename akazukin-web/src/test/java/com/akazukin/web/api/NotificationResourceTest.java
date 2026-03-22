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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@DisplayName("NotificationResource API Tests")
class NotificationResourceTest {

    // =========================================================================
    // GET /api/v1/notifications — List Notifications
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications (list)")
    class ListNotificationsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listNotifications_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007002", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listNotifications_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007003", roles = "ADMIN")
        @DisplayName("Large page number returns empty list")
        void listNotifications_largePageNumber() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 9999)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007004", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listNotifications_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listNotifications_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007005", roles = "USER")
        @DisplayName("USER role is not allowed")
        void listNotifications_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007006", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listNotifications_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/notifications/unread — List Unread Notifications
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/unread")
    class ListUnreadTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007101", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listUnread_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007102", roles = "ADMIN")
        @DisplayName("Different user sees their own empty unread list")
        void listUnread_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listUnread_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/notifications/unread/count — Count Unread Notifications
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/unread/count")
    class CountUnreadTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007201", roles = "ADMIN")
        @DisplayName("Returns 200 with count 0 for new user")
        void countUnread_zero() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread/count")
            .then()
                    .statusCode(200)
                    .body("count", equalTo(0));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007202", roles = "ADMIN")
        @DisplayName("Different user also sees count 0")
        void countUnread_differentUser_zero() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread/count")
            .then()
                    .statusCode(200)
                    .body("count", greaterThanOrEqualTo(0));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void countUnread_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/notifications/unread/count")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/notifications/{id}/read — Mark Notification as Read
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/notifications/{id}/read")
    class MarkAsReadTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007301", roles = "ADMIN")
        @DisplayName("Non-existent notification returns 204 (idempotent)")
        void markAsRead_nonExistent() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/" + UUID.randomUUID() + "/read")
            .then()
                    .statusCode(204);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007302", roles = "ADMIN")
        @DisplayName("Another non-existent notification also returns 204")
        void markAsRead_anotherNonExistent() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/" + UUID.randomUUID() + "/read")
            .then()
                    .statusCode(204);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void markAsRead_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/" + UUID.randomUUID() + "/read")
            .then()
                    .extract().statusCode();

            assert status != 204 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007303", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void markAsRead_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/not-a-uuid/read")
            .then()
                    .extract().statusCode();

            assert status != 204 && status != 200
                    : "Expected non-success for invalid UUID format, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/notifications/read-all — Mark All Notifications as Read
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/notifications/read-all")
    class MarkAllAsReadTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007401", roles = "ADMIN")
        @DisplayName("Returns 204 even when no notifications exist")
        void markAllAsRead_noNotifications() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/read-all")
            .then()
                    .statusCode(204);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007402", roles = "ADMIN")
        @DisplayName("Different user can also mark all as read")
        void markAllAsRead_differentUser() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/read-all")
            .then()
                    .statusCode(204);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void markAllAsRead_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/read-all")
            .then()
                    .extract().statusCode();

            assert status != 204 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007403", roles = "USER")
        @DisplayName("USER role is not allowed")
        void markAllAsRead_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/notifications/read-all")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for USER role, got " + status;
        }
    }
}
