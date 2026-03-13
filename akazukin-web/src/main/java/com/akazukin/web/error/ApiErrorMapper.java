package com.akazukin.web.error;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.exception.SnsApiException;
import jakarta.ws.rs.core.Response;

public final class ApiErrorMapper {

    private static final int HTTP_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final int HTTP_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    private static final int HTTP_BAD_GATEWAY = Response.Status.BAD_GATEWAY.getStatusCode();
    private static final int HTTP_INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    private ApiErrorMapper() {
    }

    /**
     * Maps specific domain exceptions to HTTP status codes.
     * <ul>
     *   <li>PostNotFoundException - 404</li>
     *   <li>AccountNotFoundException - 404</li>
     *   <li>SnsApiException - 502 (Bad Gateway)</li>
     *   <li>DomainException - 400</li>
     *   <li>Any other - 500</li>
     * </ul>
     */
    public static int mapToHttpStatus(DomainException exception) {
        if (exception instanceof PostNotFoundException) {
            return HTTP_NOT_FOUND;
        }
        if (exception instanceof AccountNotFoundException) {
            return HTTP_NOT_FOUND;
        }
        if (exception instanceof SnsApiException) {
            return HTTP_BAD_GATEWAY;
        }
        return HTTP_BAD_REQUEST;
    }

    public static int mapToHttpStatus(Exception exception) {
        if (exception instanceof DomainException domainException) {
            return mapToHttpStatus(domainException);
        }
        return HTTP_INTERNAL_SERVER_ERROR;
    }
}
