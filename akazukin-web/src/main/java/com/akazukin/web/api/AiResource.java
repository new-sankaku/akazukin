package com.akazukin.web.api;

import com.akazukin.application.dto.AiCompareRequestDto;
import com.akazukin.application.dto.AiGenerateRequestDto;
import com.akazukin.application.dto.AiPersonaRequestDto;
import com.akazukin.application.dto.AiTaskProviderSettingsRequestDto;
import com.akazukin.application.dto.AiTryoutRequestDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.usecase.AiContentUseCase;
import com.akazukin.application.usecase.AiSettingsUseCase;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AiResource {

    @Inject
    AiContentUseCase aiContentUseCase;

    @Inject
    AiSettingsUseCase aiSettingsUseCase;

    @Context
    SecurityContext securityContext;


    @POST
    @Path("/generate")
    public Response generate(AiGenerateRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = aiContentUseCase.generate(userId, request);
        return Response.ok(result).build();
    }

    @POST
    @Path("/compare")
    public Response compareGenerate(AiCompareRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = aiContentUseCase.compareGenerate(userId, request);
        return Response.ok(result).build();
    }

    @POST
    @Path("/tryout")
    public Response tryout(AiTryoutRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = aiContentUseCase.tryout(userId, request);
        return Response.ok(result).build();
    }

    @GET
    @Path("/personas")
    public Response listPersonas() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
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
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
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
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var persona = aiContentUseCase.updatePersona(id, userId, request);
        return Response.ok(persona).build();
    }

    @DELETE
    @Path("/personas/{id}")
    public Response deletePersona(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        aiContentUseCase.deletePersona(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/settings/ollama/status")
    public Response getOllamaStatus() {
        var status = aiSettingsUseCase.getOllamaStatus();
        return Response.ok(status).build();
    }

    @POST
    @Path("/settings/ollama/reconnect")
    public Response reconnectOllama() {
        var status = aiSettingsUseCase.reconnectOllama();
        return Response.ok(status).build();
    }

    @GET
    @Path("/settings/task-providers")
    public Response getTaskProviderSettings() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var settings = aiSettingsUseCase.getTaskProviderSettings(userId);
        return Response.ok(settings).build();
    }

    @PUT
    @Path("/settings/task-providers")
    public Response saveTaskProviderSettings(AiTaskProviderSettingsRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var settings = aiSettingsUseCase.saveTaskProviderSettings(userId, request);
        return Response.ok(settings).build();
    }

    @GET
    @Path("/settings/cost-monitor")
    public Response getCostMonitor() {
        var monitor = aiSettingsUseCase.getCostMonitor();
        return Response.ok(monitor).build();
    }
}
