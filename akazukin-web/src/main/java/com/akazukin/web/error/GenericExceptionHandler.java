package com.akazukin.web.error;

import com.akazukin.application.dto.ErrorResponseDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionHandler.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        LOG.log(Level.SEVERE, "Unhandled exception [errorId=" + errorId + "]", exception);

        ErrorResponseDto error = ErrorResponseDto.of(
                "INTERNAL_ERROR",
                "サーバー内部エラーが発生しました (ID: " + errorId + ")",
                null
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
