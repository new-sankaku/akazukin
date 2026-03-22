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
@DisplayName("FriendResource API Tests")
class FriendResourceTest {

    // =========================================================================
    // GET /api/v1/friends — List Friends
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/friends (list)")
    class ListFriendsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005001", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list")
        void listFriends_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005002", roles = "USER")
        @DisplayName("USER role can list friends")
        void listFriends_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listFriends_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005003", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void listFriends_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005004", roles = "ADMIN")
        @DisplayName("Different user sees their own empty list")
        void listFriends_differentUser_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }
    }

    // =========================================================================
    // POST /api/v1/friends — Add Friend
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/friends (add)")
    class AddFriendTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005101", roles = "ADMIN")
        @DisplayName("Valid request returns 201")
        void addFriend_success() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"targetIdentifier\":\"@testfriend\","
                            + "\"displayName\":\"Test Friend\","
                            + "\"notes\":\"note\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(201);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005102", roles = "ADMIN")
        @DisplayName("Invalid platform returns 400 INVALID_PLATFORM")
        void addFriend_invalidPlatform() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"NONEXISTENT\","
                            + "\"targetIdentifier\":\"@someone\","
                            + "\"displayName\":\"Someone\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_PLATFORM"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005103", roles = "ADMIN")
        @DisplayName("Missing targetIdentifier returns 400 INVALID_REQUEST")
        void addFriend_missingTargetIdentifier() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"displayName\":\"Someone\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005104", roles = "ADMIN")
        @DisplayName("Empty targetIdentifier returns 400 INVALID_REQUEST")
        void addFriend_emptyTargetIdentifier() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"targetIdentifier\":\"\","
                            + "\"displayName\":\"Someone\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005105", roles = "ADMIN")
        @DisplayName("Whitespace-only targetIdentifier returns 400 INVALID_REQUEST")
        void addFriend_whitespaceTargetIdentifier() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"targetIdentifier\":\"   \","
                            + "\"displayName\":\"Someone\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005106", roles = "USER")
        @DisplayName("USER role can add friend")
        void addFriend_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"targetIdentifier\":\"@userfriend\","
                            + "\"displayName\":\"User Friend\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .statusCode(201);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void addFriend_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"platform\":\"TWITTER\","
                            + "\"targetIdentifier\":\"@someone\","
                            + "\"displayName\":\"Someone\"}")
            .when()
                    .post("/api/v1/friends")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/friends/{id} — Remove Friend
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/friends/{id}")
    class RemoveFriendTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005201", roles = "ADMIN")
        @DisplayName("Non-existent friend returns 400 NOT_FOUND")
        void removeFriend_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/friends/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005202", roles = "ADMIN")
        @DisplayName("Another non-existent friend UUID also returns 400")
        void removeFriend_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/friends/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void removeFriend_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/friends/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005203", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void removeFriend_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/friends/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/friends/engagement — Engagement Ranking
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/friends/engagement")
    class EngagementRankingTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005301", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list for new user")
        void getEngagementRanking_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/engagement")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005302", roles = "USER")
        @DisplayName("USER role can access engagement ranking")
        void getEngagementRanking_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/engagement")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getEngagementRanking_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/engagement")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/friends/{id}/timeline — Friend Timeline
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/friends/{id}/timeline")
    class TimelineTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005401", roles = "ADMIN")
        @DisplayName("Non-existent friend returns 400 NOT_FOUND")
        void getTimeline_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/" + UUID.randomUUID() + "/timeline")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTimeline_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/" + UUID.randomUUID() + "/timeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005402", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error")
        void getTimeline_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/not-a-uuid/timeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/friends/{id}/plan — Generate Relationship Plan
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/friends/{id}/plan")
    class GeneratePlanTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005501", roles = "ADMIN")
        @DisplayName("Non-existent friend returns 400 NOT_FOUND")
        void generatePlan_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/friends/" + UUID.randomUUID() + "/plan")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generatePlan_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/friends/" + UUID.randomUUID() + "/plan")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/friends/compose — Compose for Friend
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/friends/compose")
    class ComposeForFriendTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005601", roles = "ADMIN")
        @DisplayName("Non-existent friend returns 400 NOT_FOUND")
        void composeForFriend_notFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"friendId\":\"" + UUID.randomUUID() + "\","
                            + "\"purpose\":\"reply\","
                            + "\"referenceContent\":\"test\"}")
            .when()
                    .post("/api/v1/friends/compose")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void composeForFriend_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"friendId\":\"" + UUID.randomUUID() + "\","
                            + "\"purpose\":\"reply\","
                            + "\"referenceContent\":\"test\"}")
            .when()
                    .post("/api/v1/friends/compose")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/friends/bubble-map — Bubble Map
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/friends/bubble-map")
    class BubbleMapTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005701", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty bubbles for new user")
        void getBubbleMap_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/bubble-map")
            .then()
                    .statusCode(200)
                    .body("bubbles", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005702", roles = "USER")
        @DisplayName("USER role can access bubble map")
        void getBubbleMap_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/bubble-map")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getBubbleMap_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/bubble-map")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005703", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void getBubbleMap_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/friends/bubble-map")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden for VIEWER role, got " + status;
        }
    }
}
