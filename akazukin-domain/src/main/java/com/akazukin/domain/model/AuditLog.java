package com.akazukin.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {

    private UUID id;
    private UUID userId;
    private String username;
    private String httpMethod;
    private String requestPath;
    private String queryString;
    private String requestBody;
    private int responseStatus;
    private long durationMs;
    private String clientIp;
    private String userAgent;
    private Instant createdAt;
    private String category;

    public AuditLog(UUID id, UUID userId, String username, String httpMethod, String requestPath,
                    String queryString, String requestBody, int responseStatus, long durationMs,
                    String clientIp, String userAgent, Instant createdAt, String category) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.queryString = queryString;
        this.requestBody = requestBody;
        this.responseStatus = responseStatus;
        this.durationMs = durationMs;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", requestBody='" + (requestBody != null ? requestBody.substring(0, Math.min(requestBody.length(), 100)) : "null") + '\'' +
                ", responseStatus=" + responseStatus +
                ", durationMs=" + durationMs +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
