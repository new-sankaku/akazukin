package com.akazukin.web.api;

import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.dto.PostTemplateRequestDto;
import com.akazukin.application.usecase.TemplateUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class TemplateResource {

    @Inject
    TemplateUseCase templateUseCase;

    @Context
    SecurityContext securityContext;


    @POST
    public Response create(PostTemplateRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var template = templateUseCase.createTemplate(userId, request);
        return Response.status(Response.Status.CREATED).entity(template).build();
    }

    @GET
    public Response list() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var templates = templateUseCase.listTemplates(userId);
        return Response.ok(templates).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var template = templateUseCase.getTemplate(id);
        return Response.ok(template).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        templateUseCase.deleteTemplate(id, userId);
        return Response.noContent().build();
    }
}
