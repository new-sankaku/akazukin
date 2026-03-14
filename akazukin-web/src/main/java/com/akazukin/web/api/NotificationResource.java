package com.akazukin.web.api;

import com.akazukin.application.usecase.NotificationUseCase;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Map;
import java.util.UUID;

@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class NotificationResource {

    @Inject
    NotificationUseCase notificationUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response list(@QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var notifications = notificationUseCase.listNotifications(userId, page, size);
        return Response.ok(notifications).build();
    }

    @GET
    @Path("/unread")
    public Response listUnread() {
        UUID userId = UUID.fromString(jwt.getSubject());
        var unread = notificationUseCase.listUnread(userId);
        return Response.ok(unread).build();
    }

    @GET
    @Path("/unread/count")
    public Response countUnread() {
        UUID userId = UUID.fromString(jwt.getSubject());
        long count = notificationUseCase.countUnread(userId);
        return Response.ok(Map.of("count", count)).build();
    }

    @PUT
    @Path("/{id}/read")
    public Response markAsRead(@PathParam("id") UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationUseCase.markAsRead(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/read-all")
    public Response markAllAsRead() {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationUseCase.markAllAsRead(userId);
        return Response.noContent().build();
    }
}
