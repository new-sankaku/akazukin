package com.akazukin.web.controller;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/approvals")
public class ApprovalController extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list();
        public static native TemplateInstance review(UUID approvalId);
    }

    @GET
    @Path("")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list();
    }

    @GET
    @Path("/{approvalId}/review")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance review(@PathParam("approvalId") UUID approvalId) {
        return Templates.review(approvalId);
    }
}
