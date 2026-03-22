package com.akazukin.web.api;

import com.akazukin.application.dto.AiReviewResultDto;
import com.akazukin.application.dto.ApprovalDashboardDto;
import com.akazukin.application.dto.ApprovalDecisionDto;
import com.akazukin.application.dto.ApprovalRuleDto;
import com.akazukin.application.dto.ApprovalRuleUpdateDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.usecase.ApprovalUseCase;
import com.akazukin.application.usecase.TeamApprovalUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/approvals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class ApprovalResource {

    @Inject
    ApprovalUseCase approvalUseCase;

    @Inject
    TeamApprovalUseCase teamApprovalUseCase;

    @Context
    SecurityContext securityContext;


    @GET
    @Path("/pending")
    public Response listPending(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var pendingList = approvalUseCase.listPendingApprovals(userId, page, size);
        return Response.ok(pendingList).build();
    }

    @GET
    @Path("/{id}")
    public Response getApproval(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var approval = approvalUseCase.getApproval(id, userId);
        return Response.ok(approval).build();
    }

    @POST
    @Path("/{id}/decide")
    public Response decide(@PathParam("id") UUID id, ApprovalDecisionDto decision) {
        if (decision == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        approvalUseCase.decide(id, userId, decision);
        return Response.ok().build();
    }

    @GET
    @Path("/pending/count")
    public Response countPending() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        long count = approvalUseCase.countPending(userId);
        return Response.ok(Map.of("count", count)).build();
    }

    @GET
    @Path("/rules/{teamId}")
    public Response getApprovalRules(@PathParam("teamId") UUID teamId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<ApprovalRuleDto> rules = teamApprovalUseCase.getApprovalRules(teamId, userId);
        return Response.ok(rules).build();
    }

    @PUT
    @Path("/rules/{teamId}")
    public Response updateApprovalRules(@PathParam("teamId") UUID teamId,
                                        ApprovalRuleUpdateDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<ApprovalRuleDto> updated = teamApprovalUseCase.updateApprovalRules(teamId, userId, request);
        return Response.ok(updated).build();
    }

    @GET
    @Path("/dashboard")
    public Response getDashboard() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ApprovalDashboardDto dashboard = teamApprovalUseCase.getDashboard(userId);
        return Response.ok(dashboard).build();
    }

    @GET
    @Path("/{id}/ai-review")
    public Response getAiReview(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        AiReviewResultDto review = teamApprovalUseCase.getAiReview(id, userId);
        return Response.ok(review).build();
    }

    @POST
    @Path("/{id}/ai-recheck")
    public Response recheckAi(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        AiReviewResultDto review = teamApprovalUseCase.recheckAi(id, userId);
        return Response.ok(review).build();
    }
}
