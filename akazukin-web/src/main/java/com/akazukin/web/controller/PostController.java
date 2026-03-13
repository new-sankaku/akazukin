package com.akazukin.web.controller;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/posts")
public class PostController extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance compose();
        public static native TemplateInstance list();
    }

    @GET
    @Path("/compose")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compose() {
        return Templates.compose();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list();
    }
}
