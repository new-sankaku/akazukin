package com.akazukin.web.api;

import com.akazukin.application.dto.AgentPipelineRequestDto;
import com.akazukin.application.dto.AgentStatsDto;
import com.akazukin.application.dto.AgentTaskDto;
import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.usecase.AgentPipelineUseCase;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.AgentTaskRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AgentResource {

    @Inject
    AgentPipelineUseCase agentPipelineUseCase;

    @Inject
    AgentOrchestrator agentOrchestrator;

    @Inject
    AgentTaskRepository agentTaskRepository;

    @Context
    SecurityContext securityContext;


    @POST
    @Path("/pipeline")
    public Response runPipeline(AgentPipelineRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var result = agentPipelineUseCase.runPipeline(userId, request.topic(), request.targetPlatforms());
        return Response.ok(result).build();
    }

    @POST
    @Path("/tasks")
    public Response submitTask(AgentTaskSubmitRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        AgentType agentType = AgentType.valueOf(request.agentType());
        AgentTask task = agentOrchestrator.submitTask(userId, agentType, request.input(), request.parentTaskId());
        return Response.status(Response.Status.CREATED).entity(toDto(task)).build();
    }

    @GET
    @Path("/tasks")
    public Response listTasks(@QueryParam("offset") @DefaultValue("0") int offset,
                              @QueryParam("limit") @DefaultValue("20") int limit) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        var tasks = agentTaskRepository.findByUserId(userId, offset, limit)
                .stream()
                .map(this::toDto)
                .toList();
        return Response.ok(tasks).build();
    }

    @GET
    @Path("/tasks/{id}")
    public Response getTask(@PathParam("id") UUID id) {
        AgentTask task = agentOrchestrator.getTaskResult(id);
        return Response.ok(toDto(task)).build();
    }

    @GET
    @Path("/stats")
    public Response getStats() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        long total = agentTaskRepository.countByUserId(userId);
        long completed = agentTaskRepository.countByUserIdAndStatus(userId, "COMPLETED");
        long failed = agentTaskRepository.countByUserIdAndStatus(userId, "FAILED");

        double successRate = 0.0;
        if (completed + failed > 0) {
            successRate = (double) completed / (completed + failed) * 100.0;
        }

        var tasks = agentTaskRepository.findByUserIdOrderByCreatedAt(userId, 0, 200);
        double avgDuration = tasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCreatedAt() != null)
                .mapToLong(t -> t.getCompletedAt().toEpochMilli() - t.getCreatedAt().toEpochMilli())
                .average()
                .orElse(0.0);

        long activeCount = tasks.stream()
                .map(AgentTask::getAgentType)
                .distinct()
                .count();

        return Response.ok(new AgentStatsDto(total, avgDuration, (int) activeCount, successRate)).build();
    }

    @GET
    @Path("/tasks/{id}/children")
    public Response getChildTasks(@PathParam("id") UUID id) {
        var children = agentTaskRepository.findByParentTaskId(id)
                .stream()
                .map(this::toDto)
                .toList();
        return Response.ok(children).build();
    }

    private AgentTaskDto toDto(AgentTask task) {
        return new AgentTaskDto(
                task.getId(),
                task.getAgentType().name(),
                task.getInput(),
                task.getOutput(),
                task.getStatus(),
                task.getParentTaskId(),
                task.getCreatedAt(),
                task.getCompletedAt()
        );
    }

    public record AgentTaskSubmitRequest(
        String agentType,
        String input,
        UUID parentTaskId
    ) {
    }
}
