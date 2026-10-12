package com.planmytrip.booking_service.exception;

public class HotelNotFoundException extends RuntimeException {

    public HotelNotFoundException(String hotelId) {
        super("Hotel not found: " + hotelId);
    }

}
