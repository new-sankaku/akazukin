package com.akazukin.sdk.twitter.exception;

public class TwitterApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final String detail;

    public TwitterApiException(int statusCode, String errorCode, String message, String detail) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public TwitterApiException(int statusCode, String errorCode, String message, String detail, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }
}
