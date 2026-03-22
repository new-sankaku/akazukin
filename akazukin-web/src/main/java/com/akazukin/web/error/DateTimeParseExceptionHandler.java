package com.akazukin.web.error;

import com.akazukin.application.dto.ErrorResponseDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.format.DateTimeParseException;

@Provider
public class DateTimeParseExceptionHandler implements ExceptionMapper<DateTimeParseException> {

    @Override
    public Response toResponse(DateTimeParseException exception) {
        ErrorResponseDto error = ErrorResponseDto.of(
                "INVALID_DATE_FORMAT",
                exception.getMessage(),
                null
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
