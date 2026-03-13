package com.akazukin.web.api;

import com.akazukin.application.dto.LoginRequestDto;
import com.akazukin.application.dto.LoginResponseDto;
import com.akazukin.application.dto.RefreshRequestDto;
import com.akazukin.application.dto.RegisterRequestDto;
import com.akazukin.application.usecase.AuthUseCase;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.UserRepository;
import com.akazukin.web.security.JwtTokenService;
import com.akazukin.web.security.JwtTokenService.InvalidRefreshTokenException;
import com.akazukin.web.security.PasswordHasher;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

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

    @Inject
    UserRepository userRepository;

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

    @POST
    @Path("/refresh")
    public Response refresh(RefreshRequestDto request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"INVALID_REQUEST\",\"message\":\"Refresh token is required\"}")
                    .build();
        }

        UUID userId;
        try {
            userId = jwtTokenService.parseRefreshToken(request.refreshToken());
        } catch (InvalidRefreshTokenException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"INVALID_REFRESH_TOKEN\",\"message\":\""
                            + e.getMessage() + "\"}")
                    .build();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"USER_NOT_FOUND\","
                            + "\"message\":\"User associated with refresh token no longer exists\"}")
                    .build();
        }

        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.ok(new LoginResponseDto(accessToken, refreshToken, 900))
                .build();
    }
}
