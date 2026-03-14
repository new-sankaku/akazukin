package com.akazukin.web.api;

import com.akazukin.application.dto.FriendTargetDto;
import com.akazukin.application.dto.FriendTargetRequestDto;
import com.akazukin.application.usecase.FriendUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/friends")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class FriendResource {

    @Inject
    FriendUseCase friendUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response listFriends(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<FriendTargetDto> friends = friendUseCase.listFriends(userId);
        return Response.ok(friends).build();
    }

    @POST
    public Response addFriend(FriendTargetRequestDto request,
                              @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(jwt.getSubject());
        FriendTargetDto friend = friendUseCase.addFriend(userId, request);
        return Response.status(Response.Status.CREATED).entity(friend).build();
    }

    @DELETE
    @Path("/{id}")
    public Response removeFriend(@PathParam("id") UUID id,
                                 @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(jwt.getSubject());
        friendUseCase.removeFriend(id, userId);
        return Response.noContent().build();
    }
}
