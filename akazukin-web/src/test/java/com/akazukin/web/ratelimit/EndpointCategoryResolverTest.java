package com.akazukin.web.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndpointCategoryResolverTest {

    private EndpointCategoryResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EndpointCategoryResolver();
    }

    @Test
    void shouldReturnPostCategoryForPostEndpoints() {
        assertEquals(EndpointCategory.POST, resolver.resolve("/api/v1/posts"));
        assertEquals(EndpointCategory.POST, resolver.resolve("/api/v1/posts/123"));
    }

    @Test
    void shouldReturnAiCategoryForAiEndpoints() {
        assertEquals(EndpointCategory.AI, resolver.resolve("/api/v1/ai"));
        assertEquals(EndpointCategory.AI, resolver.resolve("/api/v1/ai/generate"));
    }

    @Test
    void shouldReturnAiCategoryForAgentEndpoints() {
        assertEquals(EndpointCategory.AI, resolver.resolve("/api/v1/agents"));
        assertEquals(EndpointCategory.AI, resolver.resolve("/api/v1/agents/pipeline"));
    }

    @Test
    void shouldReturnGeneralCategoryForOtherEndpoints() {
        assertEquals(EndpointCategory.GENERAL, resolver.resolve("/api/v1/accounts"));
        assertEquals(EndpointCategory.GENERAL, resolver.resolve("/api/v1/dashboard"));
        assertEquals(EndpointCategory.GENERAL, resolver.resolve("/api/v1/teams"));
    }

    @Test
    void shouldReturnGeneralCategoryForNullPath() {
        assertEquals(EndpointCategory.GENERAL, resolver.resolve(null));
    }
}
