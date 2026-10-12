package com.planmytrip.booking_service.service.impl;

import com.planmytrip.booking_service.client.PaymentServiceClient;
import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;
import com.planmytrip.booking_service.exception.BookingNotFoundException;
import com.planmytrip.booking_service.repository.BookingRepository;
import com.planmytrip.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link BookingService}.
 * Enforces booking lifecycle transitions, transactional boundaries,
 * module-specific cancellation policies, and refund processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    @Transactional
    public Booking createPending(Booking booking) {
        log.info("Creating pending booking for user: {}, module: {}", booking.getUserId(), booking.getModule());
        booking.setStatus(BookingStatus.PENDING);
        if (booking.getCreatedAt() == null) {
            booking.setCreatedAt(Instant.now());
        }
        booking.setUpdatedAt(Instant.now());
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking confirmWithPayment(Booking booking) {
        log.info("Processing payment charge for booking ID: {}, amount: {} {}",
                booking.getId(), booking.getAmount(), booking.getCurrency());

        PaymentServiceClient.ChargeResult charge =
                paymentServiceClient.charge(booking.getId(), booking.getAmount(), booking.getCurrency());

        if (charge.success()) {
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Payment confirmed successfully for booking ID: {}", booking.getId());
        } else {
            booking.setStatus(BookingStatus.FAILED);
            log.warn("Payment charge failed for booking ID {}: {}", booking.getId(), charge.failureReason());
        }

        booking.setUpdatedAt(Instant.now());
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getById(String bookingId) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be blank");
        }

        log.debug("Looking up booking by ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId.trim())
                .orElseThrow(() -> new BookingNotFoundException(bookingId.trim()));

        return BookingResponseDto.fromEntity(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> list(String userId, BookingModule module, String tripId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be blank");
        }

        String sanitizedUserId = userId.trim();
        String sanitizedTripId = (tripId != null && !tripId.trim().isEmpty()) ? tripId.trim() : null;

        log.debug("Querying bookings for user: {}, module: {}, tripId: {}", sanitizedUserId, module, sanitizedTripId);

        List<Booking> bookings;
        if (module != null && sanitizedTripId != null) {
            bookings = bookingRepository.findByUserIdAndModuleAndTripId(sanitizedUserId, module, sanitizedTripId);
        } else if (module != null) {
            bookings = bookingRepository.findByUserIdAndModule(sanitizedUserId, module);
        } else if (sanitizedTripId != null) {
            bookings = bookingRepository.findByUserIdAndTripId(sanitizedUserId, sanitizedTripId);
        } else {
            bookings = bookingRepository.findByUserId(sanitizedUserId);
        }

        return bookings.stream()
                .map(BookingResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CancellationResultDto cancel(String bookingId, CancelBookingRequestDto request) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Booking ID cannot be blank");
        }
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        String sanitizedBookingId = bookingId.trim();
        Booking booking = bookingRepository.findById(sanitizedBookingId)
                .orElseThrow(() -> new BookingNotFoundException(sanitizedBookingId));

        // State validation: Prevent cancelling an already terminal booking
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Attempted to cancel an already cancelled booking: {}", sanitizedBookingId);
            throw new IllegalStateException("Booking " + sanitizedBookingId + " is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.FAILED) {
            log.warn("Attempted to cancel a failed booking: {}", sanitizedBookingId);
            throw new IllegalStateException("Cannot cancel a booking with status FAILED: " + sanitizedBookingId);
        }

        boolean refundEligible = false;
        BigDecimal refundAmount = BigDecimal.ZERO;
        String refundStatus = "NOT_APPLICABLE";

        // Refund is only applicable if the booking was previously CONFIRMED and paid
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            refundEligible = isRefundEligible(booking);
            if (refundEligible) {
                refundAmount = computeRefundAmount(booking);
                if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    PaymentServiceClient.RefundResult refund =
                            paymentServiceClient.refund(booking.getId(), refundAmount, booking.getCurrency());

                    if (refund.success()) {
                        refundStatus = "INITIATED";
                        booking.setRefundAmount(refund.refundedAmount());
                        log.info("Refund initiated successfully for booking {}: amount {} {}",
                                sanitizedBookingId, refund.refundedAmount(), booking.getCurrency());
                    } else {
                        refundStatus = "FAILED";
                        log.error("Refund failed for booking ID: {}", sanitizedBookingId);
                    }
                }
            } else {
                refundStatus = "NON_REFUNDABLE";
                log.info("Booking {} is outside the free cancellation window; non-refundable policy applied.", sanitizedBookingId);
            }
        }

        // Transition status and audit cancellation
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        log.info("Booking {} successfully marked as CANCELLED. Reason: '{}', Refund Status: {}",
                sanitizedBookingId, request.getReason(), refundStatus);

        return CancellationResultDto.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.CANCELLED.name())
                .refundEligible(refundEligible)
                .refundAmount(refundAmount)
                .refundStatus(refundStatus)
                .build();
    }

    /**
     * Cancellation rules differ per module and are enforced server-side.
     * Hotel policy: free cancellation up to 48 hours prior to check-in.
     */
    private boolean isRefundEligible(Booking booking) {
        if (booking.getModule() == BookingModule.HOTEL && booking.getStartDate() != null) {
            Instant checkInStart = booking.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            long hoursToCheckIn = ChronoUnit.HOURS.between(Instant.now(), checkInStart);
            return hoursToCheckIn >= 48;
        }
        return true; // Default for other travel modules
    }

    private BigDecimal computeRefundAmount(Booking booking) {
        return booking.getAmount();
    }
}
