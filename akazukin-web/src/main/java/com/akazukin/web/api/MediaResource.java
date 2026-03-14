package com.akazukin.web.api;

import com.akazukin.application.usecase.MediaUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api/v1/media")
@RolesAllowed("ADMIN")
public class MediaResource {

    @Inject
    MediaUseCase mediaUseCase;

    @Inject
    JsonWebToken jwt;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@RestForm("file") FileUpload file) {
        UUID userId = UUID.fromString(jwt.getSubject());
        byte[] data;
        try {
            data = Files.readAllBytes(file.filePath());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file: " + file.fileName(), e);
        }
        var result = mediaUseCase.upload(userId, file.fileName(), file.contentType(), data);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var mediaList = mediaUseCase.listAssets(userId, page, size);
        return Response.ok(mediaList).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        mediaUseCase.deleteAsset(id, userId);
        return Response.noContent().build();
    }
}
