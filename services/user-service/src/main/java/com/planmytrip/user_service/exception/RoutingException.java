package com.planmytrip.user_service.exception;

/**
 * Thrown by the routing integration layer when:
 *  - The external directions API cannot be reached
 *  - The API returns a non-OK status (ZERO_RESULTS, NOT_FOUND, etc.)
 *  - The response JSON cannot be parsed
 *
 * Caught and mapped to HTTP 502 Bad Gateway by GlobalExceptionHandler.
 */
public class RoutingException extends RuntimeException {

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}