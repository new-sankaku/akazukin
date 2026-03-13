package com.akazukin.web.api;

import com.akazukin.application.dto.PostRequestDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.usecase.PostUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
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

    @POST
    public Response createPost(PostRequestDto request, @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PostResponseDto response = postUseCase.createPost(userId, request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public Response listPosts(@QueryParam("page") @DefaultValue("0") int page,
                              @QueryParam("size") @DefaultValue("20") int size,
                              @Context SecurityContext securityContext) {
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
    public Response deletePost(@PathParam("id") UUID id, @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        postUseCase.deletePost(id, userId);
        return Response.noContent().build();
    }
}
