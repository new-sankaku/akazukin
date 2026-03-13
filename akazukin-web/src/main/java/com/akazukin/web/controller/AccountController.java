package com.akazukin.web.controller;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/accounts")
public class AccountController extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list();
        public static native TemplateInstance connect();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list();
    }

    @GET
    @Path("/connect")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance connect() {
        return Templates.connect();
    }
}
