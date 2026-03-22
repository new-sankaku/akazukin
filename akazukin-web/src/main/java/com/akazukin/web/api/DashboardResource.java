package com.akazukin.web.api;

import com.akazukin.application.dto.AgentPerformanceDto;
import com.akazukin.application.dto.AnalyticsResponseDto;
import com.akazukin.application.dto.CrossPlatformHeatmapDto;
import com.akazukin.application.dto.FireWatchSummaryDto;
import com.akazukin.application.dto.GrowthAdviceDto;
import com.akazukin.application.dto.PersonaPerformanceDto;
import com.akazukin.application.dto.PlatformCorrelationDto;
import com.akazukin.application.dto.PlatformSuccessRateDto;
import com.akazukin.application.dto.PodMonitorDto;
import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.QueueStatusDto;
import com.akazukin.application.dto.RiskAnalysisDto;
import com.akazukin.application.dto.SeasonalAnalysisDto;
import com.akazukin.application.dto.TrendWordDto;
import com.akazukin.application.usecase.AnalyticsUseCase;
import com.akazukin.application.usecase.DashboardUseCase;
import com.akazukin.application.usecase.GrowthStrategyUseCase;
import com.akazukin.domain.model.DashboardSummary;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "USER"})
public class DashboardResource {

    private static final int TIMELINE_MAX_LIMIT = 100;
    private static final int TIMELINE_DEFAULT_LIMIT = 20;
    private static final int POD_MONITOR_MAX_LIMIT = 50;
    private static final int POD_MONITOR_DEFAULT_LIMIT = 20;
    private static final int PLATFORM_TIMELINE_MAX_DAYS = 90;
    private static final int PLATFORM_TIMELINE_DEFAULT_DAYS = 7;
    private static final int TREND_TIMELINE_MAX_DAYS = 90;
    private static final int TREND_TIMELINE_DEFAULT_DAYS = 30;

    @Inject
    DashboardUseCase dashboardUseCase;

    @Inject
    AnalyticsUseCase analyticsUseCase;

    @Inject
    GrowthStrategyUseCase growthStrategyUseCase;

    @Context
    SecurityContext securityContext;


    @GET
    @Path("/summary")
    public Response getSummary() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        DashboardSummary summary = dashboardUseCase.getSummary(userId);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/analytics")
    public Response getAnalytics() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        AnalyticsResponseDto analytics = analyticsUseCase.getAnalytics(userId);
        return Response.ok(analytics).build();
    }

    @GET
    @Path("/timeline")
    public Response getTimeline(@QueryParam("limit") Integer limit) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        int effectiveLimit = (limit != null && limit > 0 && limit <= TIMELINE_MAX_LIMIT) ? limit : TIMELINE_DEFAULT_LIMIT;
        List<PostResponseDto> recentPosts = dashboardUseCase.getRecentPosts(userId, effectiveLimit);
        return Response.ok(recentPosts).build();
    }

    @GET
    @Path("/growth")
    public Response getGrowthAdvice(@QueryParam("accountId") UUID accountId) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        GrowthAdviceDto advice = growthStrategyUseCase.getGrowthAdvice(userId, accountId);
        return Response.ok(advice).build();
    }

    @GET
    @Path("/pod-monitor")
    public Response getPodMonitor(@QueryParam("limit") Integer limit) {
        int effectiveLimit = (limit != null && limit > 0 && limit <= POD_MONITOR_MAX_LIMIT) ? limit : POD_MONITOR_DEFAULT_LIMIT;
        PodMonitorDto monitor = dashboardUseCase.getPodMonitorLog(effectiveLimit);
        return Response.ok(monitor).build();
    }

    @GET
    @Path("/queue-status")
    public Response getQueueStatus() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        QueueStatusDto status = dashboardUseCase.getQueueStatus(userId);
        return Response.ok(status).build();
    }

    @GET
    @Path("/trend-words")
    public Response getTrendWords() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<TrendWordDto> words = dashboardUseCase.getTrendWords(userId);
        return Response.ok(words).build();
    }

    @GET
    @Path("/platform-timeline")
    public Response getPlatformTimeline(@QueryParam("days") Integer days) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        int effectiveDays = (days != null && days > 0 && days <= PLATFORM_TIMELINE_MAX_DAYS) ? days : PLATFORM_TIMELINE_DEFAULT_DAYS;
        List<PlatformSuccessRateDto> timeline = dashboardUseCase.getPlatformTimeline(userId, effectiveDays);
        return Response.ok(timeline).build();
    }

    @GET
    @Path("/fire-watch")
    public Response getFireWatch() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        FireWatchSummaryDto summary = dashboardUseCase.getFireWatchSummary(userId);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/agent-performance")
    public Response getAgentPerformance() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<AgentPerformanceDto> performance = analyticsUseCase.getAgentPerformance(userId);
        return Response.ok(performance).build();
    }

    @GET
    @Path("/persona-performance")
    public Response getPersonaPerformance() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        List<PersonaPerformanceDto> performance = analyticsUseCase.getPersonaPerformance(userId);
        return Response.ok(performance).build();
    }

    @GET
    @Path("/cross-platform-heatmap")
    public Response getCrossPlatformHeatmap() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        CrossPlatformHeatmapDto heatmap = analyticsUseCase.getCrossPlatformHeatmap(userId);
        return Response.ok(heatmap).build();
    }

    @GET
    @Path("/seasonal-analysis")
    public Response getSeasonalAnalysis() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        SeasonalAnalysisDto analysis = analyticsUseCase.getSeasonalAnalysis(userId);
        return Response.ok(analysis).build();
    }

    @GET
    @Path("/trend-timeline")
    public Response getTrendTimeline(@QueryParam("days") Integer days) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        int effectiveDays = (days != null && days > 0 && days <= TREND_TIMELINE_MAX_DAYS) ? days : TREND_TIMELINE_DEFAULT_DAYS;
        List<PlatformSuccessRateDto> timeline = analyticsUseCase.getTrendTimeline(userId, effectiveDays);
        return Response.ok(timeline).build();
    }

    @GET
    @Path("/platform-correlation")
    public Response getPlatformCorrelation() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        PlatformCorrelationDto correlation = analyticsUseCase.getPlatformCorrelation(userId);
        return Response.ok(correlation).build();
    }

    @GET
    @Path("/risk-analysis")
    public Response getRiskAnalysis() {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        RiskAnalysisDto analysis = analyticsUseCase.getRiskAnalysis(userId);
        return Response.ok(analysis).build();
    }
}
