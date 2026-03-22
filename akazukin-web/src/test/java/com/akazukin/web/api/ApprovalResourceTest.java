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
@DisplayName("ApprovalResource API Tests")
class ApprovalResourceTest {

    // =========================================================================
    // GET /api/v1/approvals/pending — List Pending Approvals
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/pending")
    class ListPendingTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005001", roles = "ADMIN")
        @DisplayName("Returns 200 with empty list when no pending approvals")
        void listPending_empty() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005002", roles = "ADMIN")
        @DisplayName("With pagination params returns 200")
        void listPending_withPagination() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005003", roles = "ADMIN")
        @DisplayName("Large page number returns empty list")
        void listPending_largePageNumber() {
            given()
                    .contentType(ContentType.JSON)
                    .queryParam("page", 9999)
                    .queryParam("size", 10)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .statusCode(200)
                    .body("$", empty());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void listPending_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005004", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void listPending_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/approvals/{id} — Get Approval
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/{id}")
    class GetApprovalTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005101", roles = "ADMIN")
        @DisplayName("Non-existent approval returns 400 APPROVAL_NOT_FOUND")
        void getApproval_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("APPROVAL_NOT_FOUND"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005102", roles = "ADMIN")
        @DisplayName("Another random non-existent approval also returns 400")
        void getApproval_anotherNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("APPROVAL_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getApproval_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005103", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error (not 200)")
        void getApproval_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/approvals/{id}/decide — Decide Approval
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/decide")
    class DecideTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005201", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void decide_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/decide")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005202", roles = "ADMIN")
        @DisplayName("Non-existent approval returns 400 APPROVAL_NOT_FOUND")
        void decide_notFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"action\":\"APPROVE\",\"comment\":\"Looks good\"}")
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/decide")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("APPROVAL_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void decide_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"action\":\"APPROVE\",\"comment\":\"OK\"}")
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/decide")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005203", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void decide_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"action\":\"APPROVE\",\"comment\":\"OK\"}")
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/decide")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/approvals/pending/count — Count Pending Approvals
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/pending/count")
    class CountPendingTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005301", roles = "ADMIN")
        @DisplayName("Returns 200 with count")
        void countPending_returns200() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending/count")
            .then()
                    .statusCode(200)
                    .body("count", notNullValue());
        }

        @Test
        @DisplayName("Without auth - blocked")
        void countPending_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending/count")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/approvals/rules/{teamId} — Get Approval Rules
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/rules/{teamId}")
    class GetApprovalRulesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005401", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void getApprovalRules_teamNotFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getApprovalRules_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005402", roles = "ADMIN")
        @DisplayName("Invalid UUID format returns error (not 200)")
        void getApprovalRules_invalidUuidFormat() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/rules/not-a-uuid")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for invalid UUID format, got " + status;
        }
    }

    // =========================================================================
    // PUT /api/v1/approvals/rules/{teamId} — Update Approval Rules
    // =========================================================================

    @Nested
    @DisplayName("PUT /api/v1/approvals/rules/{teamId}")
    class UpdateApprovalRulesTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005501", roles = "ADMIN")
        @DisplayName("Null request body returns 400 INVALID_REQUEST")
        void updateApprovalRules_nullBody_returns400() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .put("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("INVALID_REQUEST"));
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005502", roles = "ADMIN")
        @DisplayName("Non-existent team returns 400 TEAM_NOT_FOUND")
        void updateApprovalRules_teamNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body("{\"roleSettings\":[],\"aiCheckRequired\":false,\"aiAutoReject\":false,"
                            + "\"minApprovers\":1,\"approvalDeadlineHours\":24}")
            .when()
                    .put("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .statusCode(400)
                    .body("error", equalTo("TEAM_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void updateApprovalRules_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"roleSettings\":[],\"aiCheckRequired\":false,\"aiAutoReject\":false,"
                            + "\"minApprovers\":1,\"approvalDeadlineHours\":24}")
            .when()
                    .put("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005503", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void updateApprovalRules_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
                    .body("{\"roleSettings\":[],\"aiCheckRequired\":false,\"aiAutoReject\":false,"
                            + "\"minApprovers\":1,\"approvalDeadlineHours\":24}")
            .when()
                    .put("/api/v1/approvals/rules/" + UUID.randomUUID())
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/approvals/dashboard — Get Dashboard
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/dashboard")
    class GetDashboardTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005601", roles = "ADMIN")
        @DisplayName("Returns 200 with dashboard data")
        void getDashboard_returns200() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/dashboard")
            .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getDashboard_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/dashboard")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005602", roles = "VIEWER")
        @DisplayName("VIEWER role is not allowed (requires ADMIN)")
        void getDashboard_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/dashboard")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }
    }

    // =========================================================================
    // GET /api/v1/approvals/{id}/ai-review — Get AI Review
    // =========================================================================

    @Nested
    @DisplayName("GET /api/v1/approvals/{id}/ai-review")
    class GetAiReviewTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005701", roles = "ADMIN")
        @DisplayName("Non-existent approval returns 400 APPROVAL_NOT_FOUND")
        void getAiReview_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/" + UUID.randomUUID() + "/ai-review")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("APPROVAL_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void getAiReview_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/" + UUID.randomUUID() + "/ai-review")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }
    }

    // =========================================================================
    // POST /api/v1/approvals/{id}/ai-recheck — Re-check AI Review
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/ai-recheck")
    class RecheckAiTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005801", roles = "ADMIN")
        @DisplayName("Non-existent approval returns 400 APPROVAL_NOT_FOUND")
        void recheckAi_notFound() {
            given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/ai-recheck")
            .then()
                    .statusCode(400)
                    .body("error", equalTo("APPROVAL_NOT_FOUND"));
        }

        @Test
        @DisplayName("Without auth - blocked")
        void recheckAi_withoutAuth_blocked() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/ai-recheck")
            .then()
                    .extract().statusCode();

            assert status != 200 : "Expected non-200 for unauthenticated access, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005802", roles = "USER")
        @DisplayName("USER role is not allowed (requires ADMIN)")
        void recheckAi_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .post("/api/v1/approvals/" + UUID.randomUUID() + "/ai-recheck")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }
    }

    // =========================================================================
    // Role-based access tests
    // =========================================================================

    @Nested
    @DisplayName("Role-based access control")
    class RoleTests {

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005901", roles = "USER")
        @DisplayName("USER role cannot access pending list")
        void pendingList_userRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for USER role, got " + status;
        }

        @Test
        @TestSecurity(user = "00000000-0000-0000-0000-000000005902", roles = "VIEWER")
        @DisplayName("VIEWER role cannot access pending list")
        void pendingList_viewerRole_forbidden() {
            int status = given()
                    .contentType(ContentType.JSON)
            .when()
                    .get("/api/v1/approvals/pending")
            .then()
                    .extract().statusCode();

            assert status == 403 || status == 401 || status == 302 || status == 404
                    : "Expected forbidden/unauthorized for VIEWER role, got " + status;
        }
    }
}
