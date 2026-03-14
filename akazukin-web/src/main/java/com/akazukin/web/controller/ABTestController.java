package com.akazukin.web.controller;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ab-tests")
public class ABTestController extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list();
    }

    @GET
    @Path("")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list();
    }
}
