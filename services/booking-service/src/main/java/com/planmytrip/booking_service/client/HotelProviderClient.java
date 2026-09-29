package com.planmytrip.booking_service.client;

import java.time.LocalDate;
import java.util.List;

import com.planmytrip.booking_service.dto.response.HotelSearchResultDto;

public interface HotelProviderClient {

    /**
     * Bridge between Planmytrip application and External Provider
     * Duffel Stays today; Booking.com Demand API / Amadeus / HotelBeds /
     * Expedia Rapid tomorrow). HotelBookingService only ever talks to THIS
     * interface — swapping providers means writing a new implementation class
     * and changing one @Primary / Spring profile, nothing else in the codebase.
     
     * @param destination
     * @param checkIn
     * @param checkOut
     * @param adults
     * @param children
     * @return
     */

    List<HotelSearchResultDto> search(String destination, LocalDate checkIn, LocalDate checkOut,
                                       int adults, int children);

    com.planmytrip.booking_service.dto.response.HotelDetailDto getDetail(String hotelId, LocalDate checkIn, LocalDate checkOut);

    com.planmytrip.booking_service.dto.response.ProviderBookingResult book(String hotelId, String roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                                                          int adults, int children, String guestFullName, String guestEmail);
}
