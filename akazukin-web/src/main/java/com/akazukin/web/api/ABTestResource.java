package com.akazukin.web.api;

import com.akazukin.application.dto.ABTestDto;
import com.akazukin.application.dto.ABTestMultiPlatformRequestDto;
import com.akazukin.application.dto.ABTestRequestDto;
import com.akazukin.application.dto.ABTestVariantGenerateRequestDto;
import com.akazukin.application.usecase.ABTestUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.util.UUID;

@Path("/api/v1/ab-tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class ABTestResource {

    @Inject
    ABTestUseCase abTestUseCase;

    @Context
    SecurityContext securityContext;


    @GET
    public Response listTests() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<ABTestDto> tests = abTestUseCase.listTests(userId);
        return Response.ok(tests).build();
    }

    @POST
    public Response createTest(ABTestRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}/complete")
    public Response completeTest(@PathParam("id") UUID id,
                                 @QueryParam("winner") String winner) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ABTestDto completed = abTestUseCase.completeTest(id, userId, winner);
        return Response.ok(completed).build();
    }

    @PUT
    @Path("/{id}/start")
    public Response startTest(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ABTestDto started = abTestUseCase.startTest(id, userId);
        return Response.ok(started).build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancelTest(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ABTestDto cancelled = abTestUseCase.cancelTest(id, userId);
        return Response.ok(cancelled).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTest(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        abTestUseCase.deleteTest(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/generate-variants")
    public Response generateVariants(ABTestVariantGenerateRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = abTestUseCase.generateVariants(userId, request);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}/prediction")
    public Response predictOutcome(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = abTestUseCase.predictOutcome(id, userId);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}/loser-analysis")
    public Response analyzeLoser(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = abTestUseCase.analyzeLoser(id, userId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/multi-platform")
    public Response generateMultiPlatformVariants(ABTestMultiPlatformRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = abTestUseCase.generateMultiPlatformVariants(userId, request);
        return Response.ok(result).build();
    }

    @GET
    @Path("/win-patterns")
    public Response analyzeWinPatterns() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = abTestUseCase.analyzeWinPatterns(userId);
        return Response.ok(result).build();
    }
}
