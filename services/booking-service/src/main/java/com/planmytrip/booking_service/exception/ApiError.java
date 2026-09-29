package com.planmytrip.booking_service.exception;

import java.time.Instant;

import lombok.Getter;

/** Uniform error body fronted can map the UI via error code */

@Getter 
public class ApiError {

    private final Instant timestamp = Instant.now();
    private final int status;
    private final String errorCode;
    private final String message;

    public ApiError(int status, String errorCode, String message) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }
}
