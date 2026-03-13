package com.akazukin.domain.exception;

import com.akazukin.domain.model.SnsPlatform;

public class SnsApiException extends DomainException {

    private final SnsPlatform platform;
    private final int statusCode;
    private final String detail;

    public SnsApiException(SnsPlatform platform, int statusCode, String detail) {
        super("SNS_API_ERROR", buildMessage(platform, statusCode, detail));
        this.platform = platform;
        this.statusCode = statusCode;
        this.detail = detail;
    }

    private static String buildMessage(SnsPlatform platform, int statusCode, String detail) {
        return platform.name() + " API error (status " + statusCode + "): " + detail;
    }

    @Override
    public String getMessage() {
        return buildMessage(platform, statusCode, detail);
    }

    public SnsPlatform getPlatform() {
        return platform;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDetail() {
        return detail;
    }
}
