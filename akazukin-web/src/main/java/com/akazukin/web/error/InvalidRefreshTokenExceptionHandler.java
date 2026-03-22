package com.akazukin.web.error;

import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.web.security.JwtTokenService.InvalidRefreshTokenException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidRefreshTokenExceptionHandler implements ExceptionMapper<InvalidRefreshTokenException> {

    @Override
    public Response toResponse(InvalidRefreshTokenException exception) {
        ErrorResponseDto error = ErrorResponseDto.of(
                "INVALID_REFRESH_TOKEN",
                exception.getMessage(),
                null
        );
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
