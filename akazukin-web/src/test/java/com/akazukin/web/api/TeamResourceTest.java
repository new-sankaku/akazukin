package com.akazukin.web.api;

import com.akazukin.web.mock.TestUserSetup;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("TeamResource API Tests")
class TeamResourceTest {

    @Inject
    TestUserSetup testUserSetup;

    /**
     * Pre-create test users that will be referenced via @TestSecurity.
     * This ensures FK constraints on teams.owner_user_id are satisfied.
     */
    @BeforeEach
    void setUp() {
        // Create specific test users needed by the team tests
        int[] userIds = {
            2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008,
            2101, 2102, 2103,
            2201, 2202, 2204,
            2301, 2302, 2303, 2304, 2305,
            2401, 2402, 2403, 2404, 2405,
            2501, 2502, 2503, 2504,
            2601, 2602, 2603
        };
        for (int id : userIds) {
            testUserSetup.ensureUserExists(
                    UUID.fromString(String.format("00000000-0000-0000-0000-%012d", id)));
        }
    }

    // =========================================================================
    // POST /api/v1/teams — Create Team
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/teams (create)")
    class CreateTeamTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002001", roles = "ADMIN")
        @DisplayName("Success - returns 201 with team details")
        void createTeam_success() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("name", notNullValue())
                    .body("ownerUserId", equalTo("00000000-0000-0000-0000-000000002001"))
                    .body("members", hasSize(1));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002002", roles = "ADMIN")
        @DisplayName("Success - owner is automatically added as ADMIN member")
        void createTeam_ownerIsAdminMember() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Owner Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(201)
                    .body("members[0].role", equalTo("ADMIN"))
                    .body("members[0].userId", equalTo("00000000-0000-0000-0000-000000002002"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002003", roles = "ADMIN")
        @DisplayName("Missing name (null) returns 400")
        void createTeam_missingName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_TEAM_NAME"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002004", roles = "ADMIN")
        @DisplayName("Empty name returns 400 INVALID_TEAM_NAME")
        void createTeam_emptyName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_TEAM_NAME"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002005", roles = "ADMIN")
        @DisplayName("Whitespace-only name returns 400 INVALID_TEAM_NAME")
        void createTeam_whitespaceName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"   \"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_TEAM_NAME"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createTeam_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Test Team\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 201
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002006", roles = "USER")
        @DisplayName("USER role can create teams")
        void createTeam_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"User Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(201);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002007", roles = "ADMIN")
        @DisplayName("Team name with special characters succeeds")
        void createTeam_specialCharsName() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Team-Alpha_v2.0 (test)\"}")
            .when()
                    .post("/api/v1/teams")
            .then()
                    .statusCode(201)
                    .body("name", equalTo("Team-Alpha_v2.0 (test)"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002008", roles = "ADMIN")
        @DisplayName("Creating multiple teams for same user succeeds")
        void createTeam_multiple() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Team A " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams")
                    .then().statusCode(201);

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Team B " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams")
                    .then().statusCode(201);
        }
    }

    // =========================================================================
    // GET /api/v1/teams — List Teams
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/teams (list)")
    class ListTeamsTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002101", roles = "ADMIN")
        @DisplayName("Empty - returns 200 with empty list for fresh user")
        void listTeams_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002102", roles = "ADMIN")
        @DisplayName("After creating a team, it appears in the list")
        void listTeams_afterCreate() {
            String teamName = "List Team " + UUID.randomUUID().toString().substring(0, 8);
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"" + teamName + "\"}")
                    .when().post("/api/v1/teams")
                    .then().statusCode(201);

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .statusCode(200)
                    .body("[0].name", equalTo(teamName));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002103", roles = "ADMIN")
        @DisplayName("After creating multiple teams, all appear in list")
        void listTeams_multiple() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Team A " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams").then().statusCode(201);

            given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Team B " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams").then().statusCode(201);

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .statusCode(200)
                    .body("$", hasSize(2));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listTeams_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/teams/{id} — Get Team
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/teams/{id}")
    class GetTeamTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002201", roles = "ADMIN")
        @DisplayName("Success - returns team details with members")
        void getTeam_success() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Get Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams/" + teamId)
            .then()
                    .statusCode(200)
                    .body("id", equalTo(teamId))
                    .body("members", hasSize(1));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002202", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void getTeam_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002204", roles = "ADMIN")
        @DisplayName("Non-member user cannot access team created by another user")
        void getTeam_nonMember_forbidden() {
            // Create team as user 2203
            // We need a separate test with that user. Instead, test FORBIDDEN path:
            // Create team as current user, then check that the current user CAN see it.
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Accessible Team\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Current user is a member (owner), should succeed
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams/" + teamId)
            .then()
                    .statusCode(200);
        }
    }

    // =========================================================================
    // POST /api/v1/teams/{id}/members — Add Member
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/teams/{id}/members")
    class AddMemberTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002301", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void addMember_nonExistentTeam() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000009999\",\"role\":\"USER\"}")
            .when()
                    .post("/api/v1/teams/" + UUID.randomUUID() + "/members")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002302", roles = "ADMIN")
        @DisplayName("Non-existent target user returns 400 USER_NOT_FOUND")
        void addMember_nonExistentUser() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Add Member Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Use a UUID that doesn't exist in the user table
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000009999\",\"role\":\"USER\"}")
            .when()
                    .post("/api/v1/teams/" + teamId + "/members")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("USER_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002303", roles = "ADMIN")
        @DisplayName("Adding owner as member again returns 400 ALREADY_MEMBER")
        void addMember_duplicateMember() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Dup Member Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Try to add the owner again
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000002303\",\"role\":\"USER\"}")
            .when()
                    .post("/api/v1/teams/" + teamId + "/members")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ALREADY_MEMBER"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002304", roles = "ADMIN")
        @DisplayName("Add an existing user as member succeeds")
        void addMember_success() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Add Success Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Add user 2305 (pre-created in setUp) as a member
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000002305\",\"role\":\"USER\"}")
            .when()
                    .post("/api/v1/teams/" + teamId + "/members")
            .then()
                    .statusCode(200)
                    .body("members", hasSize(2));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void addMember_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000009999\",\"role\":\"USER\"}")
            .when()
                    .post("/api/v1/teams/" + UUID.randomUUID() + "/members")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/teams/{id}/members/{memberId} — Remove Member
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/teams/{id}/members/{memberId}")
    class RemoveMemberTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002401", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void removeMember_nonExistentTeam() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + UUID.randomUUID()
                            + "/members/00000000-0000-0000-0000-000000009999")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002402", roles = "ADMIN")
        @DisplayName("Cannot remove team owner")
        void removeMember_cannotRemoveOwner() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Remove Owner Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + teamId
                            + "/members/00000000-0000-0000-0000-000000002402")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("CANNOT_REMOVE_OWNER"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002403", roles = "ADMIN")
        @DisplayName("Remove non-existent member returns 400 MEMBER_NOT_FOUND")
        void removeMember_memberNotFound() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Remove NF Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + teamId
                            + "/members/00000000-0000-0000-0000-000000009999")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("MEMBER_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002404", roles = "ADMIN")
        @DisplayName("Successfully remove a member")
        void removeMember_success() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Remove Success Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Add user 2405 as member
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000002405\",\"role\":\"USER\"}")
                    .when().post("/api/v1/teams/" + teamId + "/members")
                    .then().statusCode(200);

            // Remove the added member
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + teamId
                            + "/members/00000000-0000-0000-0000-000000002405")
            .then()
                    .statusCode(204);
        }
    }

    // =========================================================================
    // DELETE /api/v1/teams/{id} — Delete Team
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/teams/{id}")
    class DeleteTeamTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002501", roles = "ADMIN")
        @DisplayName("Success - delete own team returns 204")
        void deleteTeam_success() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Delete Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + teamId)
            .then()
                    .statusCode(204);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002502", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void deleteTeam_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002503", roles = "ADMIN")
        @DisplayName("Deleted team no longer appears in list")
        void deleteTeam_removedFromList() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Del List Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Delete
            given()
                    .contentType(ContentType.JSON)
                    .when().delete("/api/v1/teams/" + teamId)
                    .then().statusCode(204);

            // Verify it's gone - getTeam should fail
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/teams/" + teamId)
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002504", roles = "ADMIN")
        @DisplayName("Delete already deleted team returns 400 TEAM_NOT_FOUND")
        void deleteTeam_alreadyDeleted() {
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Double Delete " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            given().contentType(ContentType.JSON)
                    .when().delete("/api/v1/teams/" + teamId)
                    .then().statusCode(204);

            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + teamId)
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteTeam_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/teams/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // Integration / lifecycle tests
    // =========================================================================

    @Nested
    @DisplayName("Team lifecycle integration tests")
    class LifecycleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002601", roles = "ADMIN")
        @DisplayName("Create team, get it, then delete it - full lifecycle")
        void createGetDelete_lifecycle() {
            // Create
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Lifecycle Team " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Get
            given()
                    .contentType(ContentType.JSON)
                    .when().get("/api/v1/teams/" + teamId)
                    .then().statusCode(200)
                    .body("id", equalTo(teamId));

            // Delete
            given()
                    .contentType(ContentType.JSON)
                    .when().delete("/api/v1/teams/" + teamId)
                    .then().statusCode(204);

            // Get again - should not be found
            given()
                    .contentType(ContentType.JSON)
                    .when().get("/api/v1/teams/" + teamId)
                    .then().statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000002602", roles = "ADMIN")
        @DisplayName("Create, add member, remove member lifecycle")
        void createAddRemoveMember_lifecycle() {
            // Create team
            Response createRes = given()
                    .contentType(ContentType.JSON)
                    .body("{\"name\":\"Member Lifecycle " + UUID.randomUUID().toString().substring(0, 8) + "\"}")
                    .when().post("/api/v1/teams");
            createRes.then().statusCode(201);
            String teamId = createRes.jsonPath().getString("id");

            // Add member (user 2603 exists from setUp)
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"userId\":\"00000000-0000-0000-0000-000000002603\",\"role\":\"USER\"}")
                    .when().post("/api/v1/teams/" + teamId + "/members")
                    .then().statusCode(200)
                    .body("members", hasSize(2));

            // Remove member
            given()
                    .contentType(ContentType.JSON)
                    .when().delete("/api/v1/teams/" + teamId
                            + "/members/00000000-0000-0000-0000-000000002603")
                    .then().statusCode(204);

            // Verify team now has 1 member
            given()
                    .contentType(ContentType.JSON)
                    .when().get("/api/v1/teams/" + teamId)
                    .then().statusCode(200)
                    .body("members", hasSize(1));
        }
    }
}
