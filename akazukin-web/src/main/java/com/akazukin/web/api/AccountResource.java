package com.akazukin.web.api;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.application.usecase.AccountUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AccountResource {

    @Inject
    AccountUseCase accountUseCase;

    @GET
    public Response listAccounts(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<AccountResponseDto> accounts = accountUseCase.listAccounts(userId);
        return Response.ok(accounts).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAccount(@PathParam("id") UUID id, @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        accountUseCase.deleteAccount(id, userId);
        return Response.noContent().build();
    }
}
