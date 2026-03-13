package com.akazukin.web.api;

import com.akazukin.application.dto.AccountResponseDto;
import com.akazukin.application.dto.OAuthCallbackDto;
import com.akazukin.application.usecase.AccountUseCase;
import com.akazukin.domain.model.SnsPlatform;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
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
    public Response deleteAccount(@PathParam("id") UUID id,
                                  @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        accountUseCase.deleteAccount(id, userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{platform}/auth")
    public Response getAuthorizationUrl(@PathParam("platform") String platform,
                                        @QueryParam("callbackUrl") String callbackUrl,
                                        @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        SnsPlatform snsPlatform = SnsPlatform.valueOf(platform.toUpperCase());
        String authUrl = accountUseCase.getAuthorizationUrl(userId, snsPlatform, callbackUrl);
        return Response.ok(Map.of("authorizationUrl", authUrl)).build();
    }

    @POST
    @Path("/{platform}/callback")
    public Response handleOAuthCallback(@PathParam("platform") String platform,
                                        OAuthCallbackDto callbackDto,
                                        @QueryParam("callbackUrl") String callbackUrl,
                                        @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        SnsPlatform snsPlatform = SnsPlatform.valueOf(platform.toUpperCase());
        AccountResponseDto account = accountUseCase.connectAccount(
                userId, snsPlatform, callbackDto.code(), callbackUrl
        );
        return Response.ok(account).build();
    }
}
