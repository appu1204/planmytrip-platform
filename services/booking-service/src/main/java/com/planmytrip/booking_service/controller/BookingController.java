package com.planmytrip.booking_service.controller;

import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing universal booking operations across all travel modules
 * (Hotel, Flight, Transport, Activity).
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * GET /api/bookings/{bookingId}
     * Retrieves the booking confirmation details by booking ID across any module (Hotel, Flight, Transport, Activity).
     *
     * @param bookingId The ID of the booking to retrieve (cannot be blank)
     * @return 200 OK with BookingResponseDto containing confirmation details
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBooking(
            @PathVariable @NotBlank(message = "Booking ID cannot be blank") String bookingId) {
        log.info("Fetching booking confirmation for ID: {}", bookingId);
        return ResponseEntity.ok(bookingService.getById(bookingId));
    }

    /**
     * GET /api/bookings
     * Lists bookings for a specific user with optional filtering by travel module and trip ID.
     *
     * @param userId  The ID of the user whose bookings to fetch (required, cannot be blank)
     * @param module  Optional booking module filter (HOTEL, FLIGHT, TRANSPORT, ACTIVITY)
     * @param tripId  Optional trip identifier filter
     * @return 200 OK with list of BookingResponseDto
     */
    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> listBookings(
            @RequestParam @NotBlank(message = "userId cannot be blank") String userId,
            @RequestParam(required = false) BookingModule module,
            @RequestParam(required = false) String tripId) {
        log.info("Listing bookings for user: {}, module: {}, tripId: {}", userId, module, tripId);
        return ResponseEntity.ok(bookingService.list(userId, module, tripId));
    }

    /**
     * DELETE /api/bookings/{bookingId}
     * Cancels an existing booking, executes module-specific refund eligibility policies,
     * and triggers refund processing if eligible.
     *
     * @param bookingId The ID of the booking to cancel (cannot be blank)
     * @param request   Cancellation payload containing the cancellation reason
     * @return 200 OK with CancellationResultDto containing cancellation status and refund details
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<CancellationResultDto> cancelBooking(
            @PathVariable @NotBlank(message = "Booking ID cannot be blank") String bookingId,
            @Valid @RequestBody CancelBookingRequestDto request) {
        log.info("Received request to cancel booking: {}, reason: {}", bookingId, request.getReason());
        return ResponseEntity.ok(bookingService.cancel(bookingId, request));
    }
}