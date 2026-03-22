package com.akazukin.web.api;

import com.akazukin.application.dto.BubbleMapDto;
import com.akazukin.application.dto.FriendComposeRequestDto;
import com.akazukin.application.dto.FriendComposeResponseDto;
import com.akazukin.application.dto.FriendEngagementDto;
import com.akazukin.application.dto.FriendTargetDto;
import com.akazukin.application.dto.FriendTargetRequestDto;
import com.akazukin.application.dto.FriendTimelineResponseDto;
import com.akazukin.application.dto.RelationshipPlanDto;
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

import java.util.List;
import java.util.UUID;

@Path("/api/v1/friends")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class FriendResource {

    @Inject
    FriendUseCase friendUseCase;

    @Context
    SecurityContext securityContext;


    @GET
    public Response listFriends() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<FriendTargetDto> friends = friendUseCase.listFriends(userId);
        return Response.ok(friends).build();
    }

    @POST
    public Response addFriend(FriendTargetRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        FriendTargetDto friend = friendUseCase.addFriend(userId, request);
        return Response.status(Response.Status.CREATED).entity(friend).build();
    }

    @DELETE
    @Path("/{id}")
    public Response removeFriend(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        friendUseCase.removeFriend(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/engagement")
    public Response getEngagementRanking() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<FriendEngagementDto> ranking = friendUseCase.getEngagementRanking(userId);
        return Response.ok(ranking).build();
    }

    @GET
    @Path("/{id}/timeline")
    public Response getTimeline(@PathParam("id") UUID friendId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        FriendTimelineResponseDto timeline = friendUseCase.getTimeline(userId, friendId);
        return Response.ok(timeline).build();
    }

    @POST
    @Path("/{id}/plan")
    public Response generatePlan(@PathParam("id") UUID friendId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        RelationshipPlanDto plan = friendUseCase.generateRelationshipPlan(userId, friendId);
        return Response.ok(plan).build();
    }

    @POST
    @Path("/compose")
    public Response composeForFriend(FriendComposeRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        FriendComposeResponseDto result = friendUseCase.composeForFriend(userId, request);
        return Response.ok(result).build();
    }

    @GET
    @Path("/bubble-map")
    public Response getBubbleMap() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        BubbleMapDto map = friendUseCase.getBubbleMap(userId);
        return Response.ok(map).build();
    }
}
