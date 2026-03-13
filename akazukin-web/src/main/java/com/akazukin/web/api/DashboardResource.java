package com.akazukin.web.api;

import com.akazukin.application.usecase.DashboardUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.Map;
import java.util.UUID;

@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class DashboardResource {

    @Inject
    DashboardUseCase dashboardUseCase;

    @GET
    @Path("/summary")
    public Response getSummary(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        Map<String, Object> summary = dashboardUseCase.getSummary(userId);
        return Response.ok(summary).build();
    }
}
