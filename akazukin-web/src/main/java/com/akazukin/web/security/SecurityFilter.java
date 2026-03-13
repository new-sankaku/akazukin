package com.akazukin.web.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/",
            "/register",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/q/"
    );

    @Inject
    JsonWebToken jwt;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (isPublicPath(path)) {
            return;
        }

        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            abortUnauthorized(requestContext, "Missing or invalid Authorization header");
            return;
        }

        if (jwt.getRawToken() == null || jwt.getSubject() == null) {
            abortUnauthorized(requestContext, "Invalid or expired JWT token");
            return;
        }

        String tokenType = jwt.getClaim("type");
        if ("refresh".equals(tokenType)) {
            abortUnauthorized(requestContext, "Refresh tokens cannot be used for API access");
            return;
        }
    }

    private boolean isPublicPath(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        for (String publicPath : PUBLIC_PATH_PREFIXES) {
            if (publicPath.endsWith("/")) {
                if (normalizedPath.startsWith(publicPath)
                        || normalizedPath.equals(
                                publicPath.substring(0, publicPath.length() - 1))) {
                    return true;
                }
            } else {
                if (normalizedPath.equals(publicPath)) {
                    return true;
                }
            }
        }

        if (normalizedPath.endsWith(".js") || normalizedPath.endsWith(".css")
                || normalizedPath.endsWith(".png") || normalizedPath.endsWith(".ico")
                || normalizedPath.endsWith(".svg") || normalizedPath.endsWith(".woff2")) {
            return true;
        }

        return false;
    }

    private void abortUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                        .type("application/json")
                        .build()
        );
    }
}
