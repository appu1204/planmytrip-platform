package com.planmytrip.booking_service.service;

import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;

import java.util.List;

/**
 * Service interface governing the universal booking lifecycle (create pending, confirm with payment,
 * get confirmation, list user bookings, and process cancellations).
 *
 * <p>Enforces the rule that status transitions (PENDING -&gt; CONFIRMED/FAILED -&gt; CANCELLED)
 * are managed exclusively through this service.</p>
 */
public interface BookingService {

    /**
     * Persists a new booking in PENDING status.
     * Module-specific services call this after domain-level validations.
     *
     * @param booking The initial booking entity
     * @return The persisted booking entity with assigned ID and PENDING status
     */
    Booking createPending(Booking booking);

    /**
     * Authorizes/charges the traveler for the specified booking amount
     * and transitions booking status from PENDING to CONFIRMED (or FAILED on payment failure).
     *
     * @param booking The booking entity to confirm
     * @return The updated booking entity reflecting the final status
     */
    Booking confirmWithPayment(Booking booking);

    /**
     * Retrieves the booking confirmation details by unique booking identifier.
     *
     * @param bookingId The unique ID of the booking
     * @return BookingResponseDto containing standardized confirmation fields
     * @throws com.planmytrip.booking_service.exception.BookingNotFoundException if not found
     */
    BookingResponseDto getById(String bookingId);

    /**
     * Retrieves all bookings for a user, optionally filtered by module and/or trip ID.
     *
     * @param userId  The ID of the user (cannot be blank)
     * @param module  Optional filter for booking module (HOTEL, FLIGHT, TRANSPORT, ACTIVITY)
     * @param tripId  Optional filter for associated trip ID
     * @return List of matching bookings represented as BookingResponseDto
     */
    List<BookingResponseDto> list(String userId, BookingModule module, String tripId);

    /**
     * Cancels an existing booking, computes refund eligibility based on module cancellation rules,
     * processes refund via the payment client if eligible, and updates the booking status to CANCELLED.
     *
     * @param bookingId The ID of the booking to cancel (cannot be blank)
     * @param request   The cancellation request payload containing cancellation reason
     * @return CancellationResultDto containing status and refund information
     * @throws com.planmytrip.booking_service.exception.BookingNotFoundException if booking does not exist
     * @throws IllegalStateException if booking is already cancelled or in an invalid state
     */
    CancellationResultDto cancel(String bookingId, CancelBookingRequestDto request);
}
