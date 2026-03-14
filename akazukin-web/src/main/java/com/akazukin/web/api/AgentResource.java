package com.akazukin.web.api;

import com.akazukin.application.dto.AgentPipelineRequestDto;
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
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

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

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/pipeline")
    public Response runPipeline(AgentPipelineRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        UUID userId = UUID.fromString(jwt.getSubject());
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
        UUID userId = UUID.fromString(jwt.getSubject());
        AgentType agentType = AgentType.valueOf(request.agentType());
        AgentTask task = agentOrchestrator.submitTask(userId, agentType, request.input(), request.parentTaskId());
        return Response.status(Response.Status.CREATED).entity(toDto(task)).build();
    }

    @GET
    @Path("/tasks")
    public Response listTasks(@QueryParam("offset") @DefaultValue("0") int offset,
                              @QueryParam("limit") @DefaultValue("20") int limit) {
        UUID userId = UUID.fromString(jwt.getSubject());
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
