package com.akazukin.web.api;

import com.akazukin.application.dto.AiPlanRequestDto;
import com.akazukin.application.dto.BridgeHolidayPlanDto;
import com.akazukin.application.dto.CalendarEntryDto;
import com.akazukin.application.dto.CalendarEntryRequestDto;
import com.akazukin.application.dto.CalendarTimelineEventDto;
import com.akazukin.application.dto.EngagementHeatmapDto;
import com.akazukin.application.dto.LinkageScenarioDto;
import com.akazukin.application.dto.TimeSlotMatrixDto;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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

    @Context
    SecurityContext securityContext;


    @GET
    public Response getEntries(@QueryParam("from") String from,
                               @QueryParam("to") String to) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        Instant fromInstant = Instant.parse(from);
        Instant toInstant = Instant.parse(to);
        List<CalendarEntryDto> entries = calendarUseCase.getEntries(userId, fromInstant, toInstant);
        return Response.ok(entries).build();
    }

    @POST
    public Response createEntry(CalendarEntryRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        CalendarEntryDto created = calendarUseCase.createEntry(userId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateEntry(@PathParam("id") UUID id,
                                CalendarEntryRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        CalendarEntryDto updated = calendarUseCase.updateEntry(id, userId, request);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEntry(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        calendarUseCase.deleteEntry(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/timeline")
    public Response getTimelineEvents(@QueryParam("year") int year) {
        List<CalendarTimelineEventDto> events = calendarUseCase.getTimelineEvents(year);
        return Response.ok(events).build();
    }

    @GET
    @Path("/heatmap")
    public Response getEngagementHeatmap(@QueryParam("year") int year,
                                         @QueryParam("month") int month) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        EngagementHeatmapDto heatmap = calendarUseCase.getEngagementHeatmap(userId, year, month);
        return Response.ok(heatmap).build();
    }

    @POST
    @Path("/ai-plan")
    public Response generateAiPlan(AiPlanRequestDto request) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<CalendarEntryDto> entries = calendarUseCase.generateAiPlan(userId, request);
        return Response.status(Response.Status.CREATED).entity(entries).build();
    }

    @GET
    @Path("/bridge-plans")
    public Response getBridgeHolidayPlans(@QueryParam("year") int year) {
        List<BridgeHolidayPlanDto> plans = calendarUseCase.getBridgeHolidayPlans(year);
        return Response.ok(plans).build();
    }

    @POST
    @Path("/bridge-plans/{periodName}/apply")
    public Response applyBridgePlan(@PathParam("periodName") String periodName,
                                    @QueryParam("year") int year) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<CalendarEntryDto> entries = calendarUseCase.applyBridgePlan(userId, periodName, year);
        return Response.status(Response.Status.CREATED).entity(entries).build();
    }

    @GET
    @Path("/time-slot-matrix")
    public Response getTimeSlotMatrix() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        TimeSlotMatrixDto matrix = calendarUseCase.getTimeSlotMatrix(userId);
        return Response.ok(matrix).build();
    }

    @POST
    @Path("/time-slot-reserve")
    public Response createEntryFromSlot(@QueryParam("platform") String platform,
                                        @QueryParam("day") String day,
                                        @QueryParam("hour") String hour) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        CalendarEntryDto entry = calendarUseCase.createEntryFromSlot(userId, platform, day, hour);
        return Response.status(Response.Status.CREATED).entity(entry).build();
    }

    @GET
    @Path("/linkage-scenarios")
    public Response getLinkageScenarios() {
        List<LinkageScenarioDto> scenarios = calendarUseCase.getLinkageScenarios();
        return Response.ok(scenarios).build();
    }

    @POST
    @Path("/linkage-scenarios/{scenarioName}/apply")
    public Response applyScenario(@PathParam("scenarioName") String scenarioName) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<CalendarEntryDto> entries = calendarUseCase.applyScenario(userId, scenarioName);
        return Response.status(Response.Status.CREATED).entity(entries).build();
    }
}
