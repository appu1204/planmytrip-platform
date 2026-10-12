package com.planmytrip.booking_service.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException (String bookingId) {
        super("Booking not found: " + bookingId);
    }

}
