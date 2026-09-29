package com.planmytrip.booking_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.planmytrip.booking_service.dto.response.HotelDetailDto;
import com.planmytrip.booking_service.dto.response.HotelSearchPageResponse;

public interface HotelBookingService {

    HotelSearchPageResponse search(String destination, LocalDate checkIn, LocalDate checkOut,
                                   int adults, int children,
                                   BigDecimal minPrice, BigDecimal maxPrice,
                                   int page, int size);
    
    HotelDetailDto getDetail(String hotelId, LocalDate checkIn, LocalDate checkOut);
}
