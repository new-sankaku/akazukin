package com.akazukin.web.api;

import com.akazukin.application.dto.ABTestDto;
import com.akazukin.application.dto.ABTestRequestDto;
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
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/ab-tests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class ABTestResource {

    @Inject
    ABTestUseCase abTestUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response listTests() {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<ABTestDto> tests = abTestUseCase.listTests(userId);
        return Response.ok(tests).build();
    }

    @POST
    public Response createTest(ABTestRequestDto request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}/complete")
    public Response completeTest(@PathParam("id") UUID id,
                                 @QueryParam("winner") String winner) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ABTestDto completed = abTestUseCase.completeTest(id, userId, winner);
        return Response.ok(completed).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTest(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        abTestUseCase.deleteTest(id, userId);
        return Response.noContent().build();
    }
}
