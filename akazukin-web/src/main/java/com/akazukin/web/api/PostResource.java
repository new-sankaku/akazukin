package com.akazukin.web.api;

import com.akazukin.application.dto.ComplianceCheckResultDto;
import com.akazukin.application.dto.CrossPostRecommendDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.dto.PersonaReplyRequestDto;
import com.akazukin.application.dto.PersonaReplyResponseDto;
import com.akazukin.application.dto.PostEngagementDto;
import com.akazukin.application.dto.PostReplyDto;
import com.akazukin.application.dto.PostRequestDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.usecase.PostDetailUseCase;
import com.akazukin.application.usecase.PostUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class PostResource {

    @Inject
    PostUseCase postUseCase;

    @Inject
    PostDetailUseCase postDetailUseCase;

    @Context
    SecurityContext securityContext;


    @POST
    public Response createPost(PostRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PostResponseDto response = postUseCase.createPost(userId, request);
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    public Response updatePost(@PathParam("id") UUID id,
                               PostRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PostResponseDto response = postUseCase.updatePost(id, userId, request);
        return Response.ok(response).build();
    }

    @GET
    public Response listPosts(@QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<PostResponseDto> posts = postUseCase.listPosts(userId, page, size);
        return Response.ok(posts).build();
    }

    @GET
    @Path("/{id}")
    public Response getPost(@PathParam("id") UUID id) {
        PostResponseDto post = postUseCase.getPost(id);
        return Response.ok(post).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePost(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        postUseCase.deletePost(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/engagement")
    public Response getEngagement(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PostEngagementDto engagement = postDetailUseCase.getEngagement(id, userId);
        return Response.ok(engagement).build();
    }

    @GET
    @Path("/{id}/cross-post-recommendations")
    public Response getCrossPostRecommendations(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<CrossPostRecommendDto> recommendations = postDetailUseCase.getCrossPostRecommendations(id, userId);
        return Response.ok(recommendations).build();
    }

    @GET
    @Path("/{id}/compliance")
    public Response getComplianceCheck(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ComplianceCheckResultDto result = postDetailUseCase.getComplianceCheck(id, userId);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}/replies")
    public Response getReplies(@PathParam("id") UUID id,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<PostReplyDto> replies = postDetailUseCase.getReplies(id, userId, page, size);
        return Response.ok(replies).build();
    }

    @POST
    @Path("/{id}/replies/suggest")
    public Response suggestReply(@PathParam("id") UUID id, PersonaReplyRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PersonaReplyResponseDto response = postDetailUseCase.suggestReply(id, userId, request);
        return Response.ok(response).build();
    }
}
