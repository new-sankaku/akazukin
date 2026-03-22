package com.akazukin.web.api;

import com.akazukin.application.dto.MultiAngleResponseDto;
import com.akazukin.application.dto.NewsABTestRequestDto;
import com.akazukin.application.dto.NewsABTestResponseDto;
import com.akazukin.application.dto.NewsItemDto;
import com.akazukin.application.dto.NewsPostGeneratedDto;
import com.akazukin.application.dto.NewsPostIdeaRequestDto;
import com.akazukin.application.dto.NewsPostIdeaResponseDto;
import com.akazukin.application.dto.NewsSourceDto;
import com.akazukin.application.dto.NewsSourceRequestDto;
import com.akazukin.application.dto.TemplateMatchResponseDto;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/news")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class NewsResource {

    @Inject
    NewsPostUseCase newsPostUseCase;

    @Context
    SecurityContext securityContext;


    @GET
    @Path("/sources")
    public Response listSources() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<NewsSourceDto> sources = newsPostUseCase.listSources(userId);
        return Response.ok(sources).build();
    }

    @POST
    @Path("/sources")
    public Response addSource(NewsSourceRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        NewsSourceDto created = newsPostUseCase.addSource(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/sources/{id}")
    public Response removeSource(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        newsPostUseCase.removeSource(id, userId);
        return Response.noContent().build();
    }

    @POST
    @Path("/sources/{id}/generate")
    public Response generatePost(@PathParam("id") UUID sourceId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        NewsPostGeneratedDto generated = newsPostUseCase.fetchAndGeneratePost(userId, sourceId);
        return Response.ok(generated).build();
    }

    @POST
    @Path("/sources/{id}/fetch")
    public Response fetchArticles(@PathParam("id") UUID sourceId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<NewsItemDto> articles = newsPostUseCase.fetchArticles(userId, sourceId);
        return Response.ok(articles).build();
    }

    @GET
    @Path("/articles")
    public Response listArticles() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<NewsItemDto> articles = newsPostUseCase.listArticles(userId);
        return Response.ok(articles).build();
    }

    @POST
    @Path("/post-idea")
    public Response generatePostIdea(NewsPostIdeaRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        NewsPostIdeaResponseDto result = newsPostUseCase.generatePostIdea(userId, request);
        return Response.ok(result).build();
    }

    @POST
    @Path("/articles/{id}/multi-angle")
    public Response generateMultiAngle(@PathParam("id") UUID newsItemId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        MultiAngleResponseDto result = newsPostUseCase.generateMultiAngle(userId, newsItemId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/articles/{id}/template-match")
    public Response matchTemplates(@PathParam("id") UUID newsItemId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        TemplateMatchResponseDto result = newsPostUseCase.matchTemplates(userId, newsItemId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/ab-test")
    public Response generateABTest(NewsABTestRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        NewsABTestResponseDto result = newsPostUseCase.generateABTest(userId, request);
        return Response.ok(result).build();
    }
}
