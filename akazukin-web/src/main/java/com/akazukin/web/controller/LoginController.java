package com.akazukin.web.controller;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class LoginController extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();
        public static native TemplateInstance register();
    }

    @GET
    @Path("")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index();
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance login() {
        return Templates.index();
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance register() {
        return Templates.register();
    }
}
