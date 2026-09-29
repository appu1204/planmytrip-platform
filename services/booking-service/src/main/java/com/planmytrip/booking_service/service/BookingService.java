package com.planmytrip.booking_service.service;

import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;

import java.util.List;

/**
 * Owns the shared Booking lifecycle: create (PENDING), confirm, cancel,
 * list, get. THE ONLY CLASS ALLOWED TO CHANGE A BOOKING'S STATUS —
 * HotelBookingService and friends call into this rather than touching
 * BookingRepository's save() directly for status changes.
 */
public interface BookingService {

    /** Persists a new booking in PENDING status. Module services call this after their own validation. */
    Booking createPending(Booking booking);

    /** Charges the traveler and flips PENDING -> CONFIRMED (or -> FAILED on decline). */
    Booking confirmWithPayment(Booking booking);

    BookingResponseDto getById(String bookingId);

    List<BookingResponseDto> list(String userId, BookingModule module, String tripId);

    CancellationResultDto cancel(String bookingId, CancelBookingRequestDto request);
}

