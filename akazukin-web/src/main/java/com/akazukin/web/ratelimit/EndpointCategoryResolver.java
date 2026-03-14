package com.akazukin.web.ratelimit;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EndpointCategoryResolver {

    private static final String API_PREFIX = "/api/v1/";
    private static final String POST_PATH = API_PREFIX + "posts";
    private static final String AI_PATH = API_PREFIX + "ai";
    private static final String AGENT_PATH = API_PREFIX + "agents";

    public EndpointCategory resolve(String path) {
        if (path == null) {
            return EndpointCategory.GENERAL;
        }

        if (path.startsWith(POST_PATH)) {
            return EndpointCategory.POST;
        }

        if (path.startsWith(AI_PATH) || path.startsWith(AGENT_PATH)) {
            return EndpointCategory.AI;
        }

        return EndpointCategory.GENERAL;
    }
}
