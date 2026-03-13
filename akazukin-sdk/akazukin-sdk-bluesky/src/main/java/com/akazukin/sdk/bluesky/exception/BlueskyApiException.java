package com.akazukin.sdk.bluesky.exception;

public class BlueskyApiException extends RuntimeException {

    private final int statusCode;
    private final String error;
    private final String detail;

    public BlueskyApiException(int statusCode, String error, String message) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
        this.detail = null;
    }

    public BlueskyApiException(int statusCode, String error, String message, String detail) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
        this.detail = detail;
    }

    public BlueskyApiException(int statusCode, String error, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.error = error;
        this.detail = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }
}
