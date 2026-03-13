package com.akazukin.web.error;

import com.akazukin.domain.exception.AccountNotFoundException;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.exception.SnsApiException;

public final class ApiErrorMapper {

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
            return 404;
        }
        if (exception instanceof AccountNotFoundException) {
            return 404;
        }
        if (exception instanceof SnsApiException) {
            return 502;
        }
        return 400;
    }

    public static int mapToHttpStatus(Exception exception) {
        if (exception instanceof DomainException domainException) {
            return mapToHttpStatus(domainException);
        }
        return 500;
    }
}
