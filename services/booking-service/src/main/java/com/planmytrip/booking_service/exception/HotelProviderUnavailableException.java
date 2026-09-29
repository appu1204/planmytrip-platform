package com.planmytrip.booking_service.exception;

public class HotelProviderUnavailableException extends RuntimeException {

    public HotelProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
