package com.akazukin.web.api;

import com.akazukin.application.dto.AiGenerateRequestDto;
import com.akazukin.application.dto.AiPersonaRequestDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.usecase.AiContentUseCase;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/v1/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AiResource {

    @Inject
    AiContentUseCase aiContentUseCase;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/generate")
    public Response generate(AiGenerateRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        var result = aiContentUseCase.generate(userId, request);
        return Response.ok(result).build();
    }

    @GET
    @Path("/personas")
    public Response listPersonas() {
        UUID userId = UUID.fromString(jwt.getSubject());
        var personas = aiContentUseCase.listPersonas(userId);
        return Response.ok(personas).build();
    }

    @POST
    @Path("/personas")
    public Response createPersona(AiPersonaRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        var persona = aiContentUseCase.createPersona(userId, request);
        return Response.status(Response.Status.CREATED).entity(persona).build();
    }

    @PUT
    @Path("/personas/{id}")
    public Response updatePersona(@PathParam("id") UUID id, AiPersonaRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        var persona = aiContentUseCase.updatePersona(id, userId, request);
        return Response.ok(persona).build();
    }

    @DELETE
    @Path("/personas/{id}")
    public Response deletePersona(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        aiContentUseCase.deletePersona(id, userId);
        return Response.noContent().build();
    }
}
