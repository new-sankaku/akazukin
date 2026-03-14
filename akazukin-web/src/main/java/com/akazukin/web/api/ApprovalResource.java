package com.akazukin.web.api;

import com.akazukin.application.dto.ApprovalDecisionDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.usecase.ApprovalUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

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
    JsonWebToken jwt;

    @GET
    @Path("/pending")
    public Response listPending(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var pendingList = approvalUseCase.listPendingApprovals(userId, page, size);
        return Response.ok(pendingList).build();
    }

    @POST
    @Path("/{id}/decide")
    public Response decide(@PathParam("id") UUID id, ApprovalDecisionDto decision) {
        if (decision == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        approvalUseCase.decide(id, userId, decision);
        return Response.ok().build();
    }

    @GET
    @Path("/pending/count")
    public Response countPending() {
        UUID userId = UUID.fromString(jwt.getSubject());
        long count = approvalUseCase.countPending(userId);
        return Response.ok(Map.of("count", count)).build();
    }
}
