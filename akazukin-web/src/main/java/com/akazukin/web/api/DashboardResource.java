package com.akazukin.web.api;

import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.usecase.AnalyticsUseCase;
import com.akazukin.application.usecase.DashboardUseCase;
import com.akazukin.domain.model.DashboardSummary;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class DashboardResource {

    @Inject
    DashboardUseCase dashboardUseCase;

    @Inject
    AnalyticsUseCase analyticsUseCase;

    @GET
    @Path("/summary")
    public Response getSummary(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        DashboardSummary summary = dashboardUseCase.getSummary(userId);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/analytics")
    public Response getAnalytics(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        AnalyticsResponseDto analytics = analyticsUseCase.getAnalytics(userId);
        return Response.ok(analytics).build();
    }

    @GET
    @Path("/timeline")
    public Response getTimeline(@Context SecurityContext securityContext,
                                @QueryParam("limit") Integer limit) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        int effectiveLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        List<PostResponseDto> recentPosts = dashboardUseCase.getRecentPosts(userId, effectiveLimit);
        return Response.ok(recentPosts).build();
    }
}
