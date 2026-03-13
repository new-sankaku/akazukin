package com.akazukin.web.api;

import com.akazukin.application.dto.AddMemberRequestDto;
import com.akazukin.application.dto.TeamRequestDto;
import com.akazukin.application.dto.TeamResponseDto;
import com.akazukin.application.usecase.TeamUseCase;
import com.akazukin.domain.model.Role;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/teams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class TeamResource {

    @Inject
    TeamUseCase teamUseCase;

    @POST
    public Response createTeam(TeamRequestDto request,
                               @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        TeamResponseDto team = teamUseCase.createTeam(userId, request.name());
        return Response.created(URI.create("/api/v1/teams/" + team.id()))
                .entity(team)
                .build();
    }

    @GET
    public Response listTeams(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<TeamResponseDto> teams = teamUseCase.listTeams(userId);
        return Response.ok(teams).build();
    }

    @GET
    @Path("/{id}")
    public Response getTeam(@PathParam("id") UUID id,
                            @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        TeamResponseDto team = teamUseCase.getTeam(id, userId);
        return Response.ok(team).build();
    }

    @POST
    @Path("/{id}/members")
    public Response addMember(@PathParam("id") UUID id,
                              AddMemberRequestDto request,
                              @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        Role role = Role.valueOf(request.role());
        TeamResponseDto team = teamUseCase.addMember(id, userId, request.userId(), role);
        return Response.ok(team).build();
    }

    @DELETE
    @Path("/{id}/members/{memberId}")
    public Response removeMember(@PathParam("id") UUID id,
                                 @PathParam("memberId") UUID memberId,
                                 @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        teamUseCase.removeMember(id, userId, memberId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTeam(@PathParam("id") UUID id,
                               @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        teamUseCase.deleteTeam(id, userId);
        return Response.noContent().build();
    }
}
