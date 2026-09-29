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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.planmytrip.booking_service.service.BookingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    @Transactional
    public Booking createPending(Booking booking) {
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking confirmWithPayment(Booking booking) {
        PaymentServiceClient.ChargeResult charge =
                paymentServiceClient.charge(booking.getId(), booking.getAmount(), booking.getCurrency());

        if (charge.success()) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.FAILED);
            log.warn("Payment failed for booking {}: {}", booking.getId(), charge.failureReason());
        }
        booking.setUpdatedAt(Instant.now());
        return bookingRepository.save(booking);
    }

    @Override
    public BookingResponseDto getById(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return BookingResponseDto.fromEntity(booking);
    }

    @Override
    public List<BookingResponseDto> list(String userId, BookingModule module, String tripId) {
        List<Booking> bookings;
        if (module != null && tripId != null) {
            bookings = bookingRepository.findByUserIdAndModuleAndTripId(userId, module, tripId);
        } else if (module != null) {
            bookings = bookingRepository.findByUserIdAndModule(userId, module);
        } else if (tripId != null) {
            bookings = bookingRepository.findByUserIdAndTripId(userId, tripId);
        } else {
            bookings = bookingRepository.findByUserId(userId);
        }
        return bookings.stream().map(BookingResponseDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CancellationResultDto cancel(String bookingId, CancelBookingRequestDto request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        boolean refundEligible = isRefundEligible(booking);
        BigDecimal refundAmount = refundEligible ? computeRefundAmount(booking) : BigDecimal.ZERO;
        String refundStatus = "NOT_APPLICABLE";

        if (refundEligible && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentServiceClient.RefundResult refund =
                    paymentServiceClient.refund(booking.getId(), refundAmount, booking.getCurrency());
            refundStatus = refund.success() ? "INITIATED" : "FAILED";
            if (refund.success()) {
                booking.setRefundAmount(refund.refundedAmount());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        return CancellationResultDto.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.CANCELLED.name())
                .refundEligible(refundEligible)
                .refundAmount(refundAmount)
                .refundStatus(refundStatus)
                .build();
    }

    /**
     * Cancellation rules differ per module and are enforced HERE (server-side),
     * never left to the frontend. Hotel rule from the PDF: free cancellation
     * up to 48h before check-in; after that, the first night is non-refundable.
     */
    private boolean isRefundEligible(Booking booking) {
        if (booking.getModule() == BookingModule.HOTEL && booking.getStartDate() != null) {
            Instant checkInStart = booking.getStartDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            long hoursToCheckIn = ChronoUnit.HOURS.between(Instant.now(), checkInStart);
            return hoursToCheckIn >= 48;
        }
        return true; // other modules: extend with their own rules as they're built
    }

    private BigDecimal computeRefundAmount(Booking booking) {
        // Simplified: full refund if eligible. A fuller implementation would
        // subtract the first-night charge when cancelling inside the free window.
        return booking.getAmount();
    }
}
