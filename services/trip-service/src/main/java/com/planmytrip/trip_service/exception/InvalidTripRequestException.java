package com.planmytrip.trip_service.exception;

public class InvalidTripRequestException extends RuntimeException {
    public InvalidTripRequestException(String message) {
        super(message);
    }
}
