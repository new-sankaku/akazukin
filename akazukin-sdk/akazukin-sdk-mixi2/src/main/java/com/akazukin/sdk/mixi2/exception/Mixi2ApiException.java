package com.akazukin.sdk.mixi2.exception;

public class Mixi2ApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final String detail;

    public Mixi2ApiException(int statusCode, String errorCode, String message, String detail) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public Mixi2ApiException(int statusCode, String errorCode, String message, String detail, Throwable cause) {
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
