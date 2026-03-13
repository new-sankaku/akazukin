package com.akazukin.web.api;

import com.akazukin.application.dto.*;
import com.akazukin.application.usecase.AuthUseCase;
import com.akazukin.domain.model.User;
import com.akazukin.web.security.JwtTokenService;
import com.akazukin.web.security.PasswordHasher;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthUseCase authUseCase;

    @Inject
    JwtTokenService jwtTokenService;

    @Inject
    PasswordHasher passwordHasher;

    @POST
    @Path("/register")
    public Response register(RegisterRequestDto request) {
        User user = authUseCase.register(request);
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.status(Response.Status.CREATED)
                .entity(new LoginResponseDto(accessToken, refreshToken, 900))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequestDto request) {
        User user = authUseCase.authenticate(request.username(), request.password());
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.ok(new LoginResponseDto(accessToken, refreshToken, 900))
                .build();
    }
}
