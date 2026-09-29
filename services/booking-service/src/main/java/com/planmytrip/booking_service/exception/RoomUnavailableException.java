package com.planmytrip.booking_service.exception;

public class RoomUnavailableException extends RuntimeException {

    public RoomUnavailableException(String roomTypeId) {
        super("Room type is not available for the requested dates: " + roomTypeId);
    }

}
