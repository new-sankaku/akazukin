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

        if (requestContext.getSecurityContext().getUserPrincipal() != null) {
            try {
                if (jwt.getRawToken() != null) {
                    String tokenType = jwt.getClaim("type");
                    if ("refresh".equals(tokenType)) {
                        abortUnauthorized(requestContext, "Refresh tokens cannot be used for API access");
                        return;
                    }
                }
            } catch (IllegalStateException ignored) {
            }
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
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        for (String publicPath : PUBLIC_PATH_PREFIXES) {
            String normalizedPublic = publicPath;
            if (normalizedPublic.length() > 1 && normalizedPublic.endsWith("/")) {
                normalizedPublic = normalizedPublic.substring(0, normalizedPublic.length() - 1);
            }

            if (normalizedPublic.endsWith("/")) {
                if (normalizedPath.startsWith(normalizedPublic)) {
                    return true;
                }
            } else {
                if (normalizedPath.equals(normalizedPublic)
                        || normalizedPath.startsWith(normalizedPublic + "/")) {
                    return true;
                }
            }
        }

        if (normalizedPath.endsWith(".js") || normalizedPath.endsWith(".css")
                || normalizedPath.endsWith(".png") || normalizedPath.endsWith(".ico")
                || normalizedPath.endsWith(".svg") || normalizedPath.endsWith(".webp")
                || normalizedPath.endsWith(".woff") || normalizedPath.endsWith(".woff2")
                || normalizedPath.endsWith(".ttf") || normalizedPath.endsWith(".eot")) {
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
