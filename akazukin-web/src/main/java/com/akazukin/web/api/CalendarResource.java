package com.akazukin.web.api;

import com.akazukin.application.dto.CalendarEntryDto;
import com.akazukin.application.dto.CalendarEntryRequestDto;
import com.akazukin.application.usecase.CalendarUseCase;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/calendar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class CalendarResource {

    @Inject
    CalendarUseCase calendarUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getEntries(@QueryParam("from") String from,
                               @QueryParam("to") String to) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Instant fromInstant = Instant.parse(from);
        Instant toInstant = Instant.parse(to);
        List<CalendarEntryDto> entries = calendarUseCase.getEntries(userId, fromInstant, toInstant);
        return Response.ok(entries).build();
    }

    @POST
    public Response createEntry(CalendarEntryRequestDto request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CalendarEntryDto created = calendarUseCase.createEntry(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateEntry(@PathParam("id") UUID id,
                                CalendarEntryRequestDto request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CalendarEntryDto updated = calendarUseCase.updateEntry(id, userId, request);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEntry(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        calendarUseCase.deleteEntry(id, userId);
        return Response.noContent().build();
    }
}
