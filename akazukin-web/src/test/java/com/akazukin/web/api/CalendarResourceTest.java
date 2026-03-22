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
@DisplayName("CalendarResource API Tests")
class CalendarResourceTest {

    // =========================================================================
    // GET /api/v1/calendar — Get Entries
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar (entries)")
    class GetEntriesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007001", roles = "ADMIN")
        @DisplayName("Valid date range returns 200 with empty list")
        void getEntries_validRange_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007002", roles = "USER")
        @DisplayName("USER role can access calendar entries")
        void getEntries_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .statusCode(200);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007003", roles = "ADMIN")
        @DisplayName("Invalid from format returns error (not 200)")
        void getEntries_invalidFromFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "invalid-date")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid date format, got " + status;
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getEntries_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007004", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed")
        void getEntries_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/calendar — Create Entry
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/calendar (create)")
    class CreateEntryTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007101", roles = "ADMIN")
        @DisplayName("Valid entry returns 201")
        void createEntry_valid_returns201() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Test entry\",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"],\"color\":\"#C8A96E\"}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(201)
                    .body("title", equalTo("Test entry"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007102", roles = "ADMIN")
        @DisplayName("Missing title returns 400 INVALID_INPUT")
        void createEntry_missingTitle_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"scheduledAt\":\"2026-06-01T10:00:00Z\",\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007103", roles = "ADMIN")
        @DisplayName("Empty title returns 400 INVALID_INPUT")
        void createEntry_emptyTitle_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"\",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007104", roles = "ADMIN")
        @DisplayName("Whitespace-only title returns 400 INVALID_INPUT")
        void createEntry_whitespaceTitle_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"   \",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007105", roles = "ADMIN")
        @DisplayName("Missing scheduledAt returns 400 INVALID_INPUT")
        void createEntry_missingScheduledAt_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Test\",\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007106", roles = "ADMIN")
        @DisplayName("Entry without platforms defaults to empty platform list")
        void createEntry_noPlatforms_returns201() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"No platforms\",\"scheduledAt\":\"2026-06-01T10:00:00Z\"}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(201);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createEntry_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Test\",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .extract().statusCode();

            assert status != 201 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007107", roles = "USER")
        @DisplayName("USER role can create calendar entries")
        void createEntry_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"User entry\",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .post("/api/v1/calendar")
            .then()
                    .statusCode(201);
        }
    }

    // =========================================================================
    // PUT /api/v1/calendar/{id} — Update Entry
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/calendar/{id}")
    class UpdateEntryTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007201", roles = "ADMIN")
        @DisplayName("Non-existent entry returns 400 ENTRY_NOT_FOUND")
        void updateEntry_notFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Updated\",\"scheduledAt\":\"2026-06-01T10:00:00Z\","
                            + "\"platforms\":[\"TWITTER\"]}")
            .when()
                    .put("/api/v1/calendar/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ENTRY_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void updateEntry_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Updated\",\"scheduledAt\":\"2026-06-01T10:00:00Z\"}")
            .when()
                    .put("/api/v1/calendar/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007202", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error (not 200)")
        void updateEntry_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"title\":\"Updated\",\"scheduledAt\":\"2026-06-01T10:00:00Z\"}")
            .when()
                    .put("/api/v1/calendar/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }
    }

    // =========================================================================
    // DELETE /api/v1/calendar/{id} — Delete Entry
    // =========================================================================

    @Nested
    @DisplayName("DELETE /api/v1/calendar/{id}")
    class DeleteEntryTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007301", roles = "ADMIN")
        @DisplayName("Non-existent entry returns 400 ENTRY_NOT_FOUND")
        void deleteEntry_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/calendar/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("ENTRY_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void deleteEntry_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/calendar/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for unauthenticated DELETE, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007302", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error (not 200/204)")
        void deleteEntry_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .delete("/api/v1/calendar/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 && status != 204
                    : "Expected non-success for invalid UUID, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/calendar/timeline — Get Timeline Events
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar/timeline")
    class GetTimelineTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007401", roles = "ADMIN")
        @DisplayName("Valid year returns 200 with events")
        void getTimeline_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .get("/api/v1/calendar/timeline")
            .then()
                    .statusCode(200)
                    .body("$", not(empty()));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTimeline_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .get("/api/v1/calendar/timeline")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/calendar/heatmap — Get Engagement Heatmap
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar/heatmap")
    class GetHeatmapTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007501", roles = "ADMIN")
        @DisplayName("Valid year and month returns 200")
        void getHeatmap_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
                    .queryParam("month", 6)
            .when()
                    .get("/api/v1/calendar/heatmap")
            .then()
                    .statusCode(200)
                    .body("levels", notNullValue());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getHeatmap_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
                    .queryParam("month", 6)
            .when()
                    .get("/api/v1/calendar/heatmap")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/calendar/ai-plan — Generate AI Plan
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/calendar/ai-plan")
    class GenerateAiPlanTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007601", roles = "ADMIN")
        @DisplayName("Valid request returns 201 with entries")
        void generateAiPlan_valid_returns201() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"theme\":\"Summer campaign\",\"platforms\":[\"TWITTER\"],\"postCount\":3}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .statusCode(201)
                    .body("$.size()", equalTo(3));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007602", roles = "ADMIN")
        @DisplayName("Missing theme returns 400 INVALID_INPUT")
        void generateAiPlan_missingTheme_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"platforms\":[\"TWITTER\"],\"postCount\":3}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007603", roles = "ADMIN")
        @DisplayName("Empty platforms returns 400 INVALID_INPUT")
        void generateAiPlan_emptyPlatforms_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"theme\":\"Test\",\"platforms\":[],\"postCount\":3}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007604", roles = "ADMIN")
        @DisplayName("Post count 0 returns 400 INVALID_INPUT")
        void generateAiPlan_zeroPostCount_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"theme\":\"Test\",\"platforms\":[\"TWITTER\"],\"postCount\":0}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007605", roles = "ADMIN")
        @DisplayName("Post count exceeding 50 returns 400 INVALID_INPUT")
        void generateAiPlan_excessivePostCount_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"theme\":\"Test\",\"platforms\":[\"TWITTER\"],\"postCount\":51}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void generateAiPlan_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"theme\":\"Test\",\"platforms\":[\"TWITTER\"],\"postCount\":3}")
            .when()
                    .post("/api/v1/calendar/ai-plan")
            .then()
                    .extract().statusCode();

            assert status != 201 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/calendar/bridge-plans — Get Bridge Holiday Plans
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar/bridge-plans")
    class GetBridgePlansTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007701", roles = "ADMIN")
        @DisplayName("Valid year returns 200")
        void getBridgePlans_returns200() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .get("/api/v1/calendar/bridge-plans")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getBridgePlans_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .get("/api/v1/calendar/bridge-plans")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/calendar/bridge-plans/{periodName}/apply — Apply Bridge Plan
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/calendar/bridge-plans/{periodName}/apply")
    class ApplyBridgePlanTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007801", roles = "ADMIN")
        @DisplayName("Non-existent period returns 400 PLAN_NOT_FOUND")
        void applyBridgePlan_notFound() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .post("/api/v1/calendar/bridge-plans/NONEXISTENT_PLAN/apply")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("PLAN_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void applyBridgePlan_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("year", 2025)
            .when()
                    .post("/api/v1/calendar/bridge-plans/GW/apply")
            .then()
                    .extract().statusCode();

            assert status != 201 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/calendar/time-slot-matrix — Get Time Slot Matrix
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar/time-slot-matrix")
    class GetTimeSlotMatrixTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000007901", roles = "ADMIN")
        @DisplayName("Returns 200 with matrix data")
        void getTimeSlotMatrix_returns200() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/calendar/time-slot-matrix")
            .then()
                    .statusCode(200)
                    .body("platforms", notNullValue())
                    .body("dayLabels", notNullValue())
                    .body("hourLabels", notNullValue());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getTimeSlotMatrix_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/calendar/time-slot-matrix")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/calendar/time-slot-reserve — Create Entry From Slot
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/calendar/time-slot-reserve")
    class CreateEntryFromSlotTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008001", roles = "ADMIN")
        @DisplayName("Valid slot returns 201")
        void createEntryFromSlot_valid_returns201() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("platform", "X")
                    .queryParam("day", "\u6708")
                    .queryParam("hour", "18-21")
            .when()
                    .post("/api/v1/calendar/time-slot-reserve")
            .then()
                    .statusCode(201);
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008002", roles = "ADMIN")
        @DisplayName("Invalid day returns 400 INVALID_INPUT")
        void createEntryFromSlot_invalidDay_returns400() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("platform", "X")
                    .queryParam("day", "InvalidDay")
                    .queryParam("hour", "18-21")
            .when()
                    .post("/api/v1/calendar/time-slot-reserve")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void createEntryFromSlot_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("platform", "X")
                    .queryParam("day", "\u6708")
                    .queryParam("hour", "18-21")
            .when()
                    .post("/api/v1/calendar/time-slot-reserve")
            .then()
                    .extract().statusCode();

            assert status != 201 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/calendar/linkage-scenarios — Get Linkage Scenarios
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/calendar/linkage-scenarios")
    class GetLinkageScenariosTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008101", roles = "ADMIN")
        @DisplayName("Returns 200 with scenarios")
        void getLinkageScenarios_returns200() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/calendar/linkage-scenarios")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getLinkageScenarios_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/calendar/linkage-scenarios")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/calendar/linkage-scenarios/{scenarioName}/apply
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/calendar/linkage-scenarios/{scenarioName}/apply")
    class ApplyScenarioTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008201", roles = "ADMIN")
        @DisplayName("Non-existent scenario returns 400 SCENARIO_NOT_FOUND")
        void applyScenario_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/calendar/linkage-scenarios/NONEXISTENT_SCENARIO/apply")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("SCENARIO_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void applyScenario_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/calendar/linkage-scenarios/test/apply")
            .then()
                    .extract().statusCode();

            assert status != 201 && status != 200
                    : "Expected non-success for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // Role-based access tests
    // =========================================================================

    @Nested
    @DisplayName("Role-based access control")
    class RoleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008301", roles = "VIEWER")
        @DisplayName("VIEWER role cannot access calendar")
        void calendar_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000008302", roles = "USER")
        @DisplayName("USER role can access calendar")
        void calendar_userRole_allowed() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("from", "2025-01-01T00:00:00Z")
                    .queryParam("to", "2025-12-31T23:59:59Z")
            .when()
                    .get("/api/v1/calendar")
            .then()
                    .statusCode(200);
        }
    }
}
