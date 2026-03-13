package com.akazukin.web.error;

import com.akazukin.application.dto.ErrorResponseDto;
import com.akazukin.domain.exception.DomainException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException exception) {
        int status = ApiErrorMapper.mapToHttpStatus(exception);
        ErrorResponseDto error = ErrorResponseDto.of(
                exception.getErrorCode(),
                exception.getMessage(),
                null
        );
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
