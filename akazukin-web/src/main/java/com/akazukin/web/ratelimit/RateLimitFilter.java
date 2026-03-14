package com.akazukin.web.ratelimit;

import com.akazukin.application.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class RateLimitFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class.getName());
    private static final String REMAINING_TOKENS_PROPERTY = "rateLimitRemainingTokens";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String API_PATH_PREFIX = "/api/";
    private static final String ANONYMOUS_KEY = "anonymous";

    @Inject
    RateLimitBucketManager bucketManager;

    @Inject
    EndpointCategoryResolver categoryResolver;

    @Inject
    RateLimitConfig rateLimitConfig;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (!path.startsWith(API_PATH_PREFIX)) {
            return;
        }

        String userIdentifier = extractUserIdentifier(requestContext);
        EndpointCategory category = categoryResolver.resolve(path);
        ConsumptionProbe probe = bucketManager.tryConsume(userIdentifier, category);

        if (probe.isConsumed()) {
            requestContext.setProperty(REMAINING_TOKENS_PROPERTY, probe.getRemainingTokens());
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        LOG.log(Level.WARNING, "Rate limit exceeded for user {0} on category {1}",
                new Object[]{userIdentifier, category});

        ErrorResponseDto error = ErrorResponseDto.of(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later.",
                "Retry after " + retryAfterSeconds + " seconds"
        );

        String body = objectMapper.writeValueAsString(error);
        requestContext.abortWith(
                Response.status(Response.Status.TOO_MANY_REQUESTS)
                        .type(MediaType.APPLICATION_JSON)
                        .header(HEADER_RETRY_AFTER, retryAfterSeconds)
                        .header(HEADER_REMAINING, 0)
                        .header(HEADER_LIMIT, rateLimitConfig.getCapacity(category))
                        .entity(body)
                        .build()
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object remaining = requestContext.getProperty(REMAINING_TOKENS_PROPERTY);
        if (remaining instanceof Long remainingTokens) {
            String path = requestContext.getUriInfo().getPath();
            EndpointCategory category = categoryResolver.resolve(path);
            responseContext.getHeaders().putSingle(HEADER_REMAINING, remainingTokens);
            responseContext.getHeaders().putSingle(HEADER_LIMIT, rateLimitConfig.getCapacity(category));
        }
    }

    private String extractUserIdentifier(ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        if (securityContext != null) {
            Principal principal = securityContext.getUserPrincipal();
            if (principal != null && principal.getName() != null) {
                return principal.getName();
            }
        }
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }
        return ANONYMOUS_KEY;
    }
}
