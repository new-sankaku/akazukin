package com.akazukin.web.api;

import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.application.dto.LoginRequestDto;
import com.akazukin.application.dto.LoginResponseDto;
import com.akazukin.application.dto.RefreshRequestDto;
import com.akazukin.application.dto.RegisterRequestDto;
import com.akazukin.application.usecase.AuthUseCase;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.UserRepository;
import com.akazukin.web.security.JwtTokenService;
import com.akazukin.web.security.PasswordHasher;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
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

    @ConfigProperty(name = "akazukin.jwt.access-token-duration", defaultValue = "PT15M")
    Duration accessTokenDuration;

    @POST
    @Path("/register")
    public Response register(RegisterRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        User user = authUseCase.register(request);
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.status(Response.Status.CREATED)
                .entity(new LoginResponseDto(accessToken, refreshToken, accessTokenDuration.toSeconds()))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequestDto request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of("INVALID_REQUEST", "Request body is required", null))
                    .build();
        }
        User user = authUseCase.authenticate(request.username(), request.password());
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.ok(new LoginResponseDto(accessToken, refreshToken, accessTokenDuration.toSeconds()))
                .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(RefreshRequestDto request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDto.of(
                            "INVALID_REQUEST",
                            "Refresh token is required",
                            null))
                    .build();
        }

        UUID userId = jwtTokenService.parseRefreshToken(request.refreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.akazukin.domain.exception.AccountNotFoundException(userId));


        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);
        return Response.ok(new LoginResponseDto(accessToken, refreshToken, accessTokenDuration.toSeconds()))
                .build();
    }
}
