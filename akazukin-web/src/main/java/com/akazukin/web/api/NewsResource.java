package com.akazukin.web.api;

import com.akazukin.application.dto.NewsPostGeneratedDto;
import com.akazukin.application.dto.NewsSourceDto;
import com.akazukin.application.dto.NewsSourceRequestDto;
import com.akazukin.application.usecase.NewsPostUseCase;
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
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/news")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class NewsResource {

    @Inject
    NewsPostUseCase newsPostUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/sources")
    public Response listSources() {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<NewsSourceDto> sources = newsPostUseCase.listSources(userId);
        return Response.ok(sources).build();
    }

    @POST
    @Path("/sources")
    public Response addSource(NewsSourceRequestDto request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        NewsSourceDto created = newsPostUseCase.addSource(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/sources/{id}")
    public Response removeSource(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        newsPostUseCase.removeSource(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/sources/{id}/generate")
    public Response generatePost(@PathParam("id") UUID sourceId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        NewsPostGeneratedDto generated = newsPostUseCase.fetchAndGeneratePost(userId, sourceId);
        return Response.ok(generated).build();
    }
}
