package com.planmytrip.booking_service.service;

import com.planmytrip.booking_service.client.PaymentServiceClient;
import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.entity.Booking;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;
import com.planmytrip.booking_service.exception.BookingNotFoundException;
import com.planmytrip.booking_service.repository.BookingRepository;
import com.planmytrip.booking_service.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        sampleBooking = Booking.builder()
                .id("bkg-12345")
                .userId("usr-100")
                .tripId("trip-200")
                .module(BookingModule.HOTEL)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-HTL-12345")
                .itemName("Grand Resort")
                .startDate(LocalDate.now().plusDays(5)) // > 48h away
                .endDate(LocalDate.now().plusDays(8))
                .amount(new BigDecimal("15000.00"))
                .currency("INR")
                .build();
    }

    // =========================================================================
    // createPending & confirmWithPayment Tests
    // =========================================================================

    @Test
    @DisplayName("createPending sets status to PENDING and saves")
    void createPending_success() {
        Booking booking = Booking.builder().userId("usr-1").module(BookingModule.HOTEL).build();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createPending(booking);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("confirmWithPayment marks CONFIRMED on successful charge")
    void confirmWithPayment_chargeSuccess() {
        PaymentServiceClient.ChargeResult chargeResult = new PaymentServiceClient.ChargeResult(true, "chg_123", null);
        when(paymentServiceClient.charge("bkg-12345", sampleBooking.getAmount(), "INR")).thenReturn(chargeResult);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.confirmWithPayment(sampleBooking);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("confirmWithPayment marks FAILED on declined charge")
    void confirmWithPayment_chargeFailed() {
        PaymentServiceClient.ChargeResult chargeResult = new PaymentServiceClient.ChargeResult(false, null, "Card declined");
        when(paymentServiceClient.charge("bkg-12345", sampleBooking.getAmount(), "INR")).thenReturn(chargeResult);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.confirmWithPayment(sampleBooking);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.FAILED);
        verify(bookingRepository).save(sampleBooking);
    }

    // =========================================================================
    // getById Tests
    // =========================================================================

    @Test
    @DisplayName("getById returns BookingResponseDto when found")
    void getById_found() {
        when(bookingRepository.findById("bkg-12345")).thenReturn(Optional.of(sampleBooking));

        BookingResponseDto response = bookingService.getById("bkg-12345");

        assertThat(response.getBookingId()).isEqualTo("bkg-12345");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getReferenceNo()).isEqualTo("PMT-HTL-12345");
    }

    @Test
    @DisplayName("getById throws BookingNotFoundException when not found")
    void getById_notFound() {
        when(bookingRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getById("unknown"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("getById throws IllegalArgumentException when id is blank")
    void getById_blankId() {
        assertThatThrownBy(() -> bookingService.getById("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // list Tests
    // =========================================================================

    @Test
    @DisplayName("list filters by userId only")
    void list_byUserId() {
        when(bookingRepository.findByUserId("usr-100")).thenReturn(List.of(sampleBooking));

        List<BookingResponseDto> list = bookingService.list("usr-100", null, null);

        assertThat(list).hasSize(1);
        verify(bookingRepository).findByUserId("usr-100");
    }

    @Test
    @DisplayName("list filters by userId and module")
    void list_byUserIdAndModule() {
        when(bookingRepository.findByUserIdAndModule("usr-100", BookingModule.HOTEL))
                .thenReturn(List.of(sampleBooking));

        List<BookingResponseDto> list = bookingService.list("usr-100", BookingModule.HOTEL, null);

        assertThat(list).hasSize(1);
        verify(bookingRepository).findByUserIdAndModule("usr-100", BookingModule.HOTEL);
    }

    @Test
    @DisplayName("list filters by userId, module, and tripId")
    void list_byUserIdModuleAndTripId() {
        when(bookingRepository.findByUserIdAndModuleAndTripId("usr-100", BookingModule.HOTEL, "trip-200"))
                .thenReturn(List.of(sampleBooking));

        List<BookingResponseDto> list = bookingService.list("usr-100", BookingModule.HOTEL, "trip-200");

        assertThat(list).hasSize(1);
        verify(bookingRepository).findByUserIdAndModuleAndTripId("usr-100", BookingModule.HOTEL, "trip-200");
    }

    @Test
    @DisplayName("list throws IllegalArgumentException when userId is blank")
    void list_blankUserId() {
        assertThatThrownBy(() -> bookingService.list("  ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // cancel Tests
    // =========================================================================

    @Test
    @DisplayName("cancel initiates refund when hotel booking is cancelled > 48h before check-in")
    void cancel_hotelFreeWindow_refundInitiated() {
        when(bookingRepository.findById("bkg-12345")).thenReturn(Optional.of(sampleBooking));
        PaymentServiceClient.RefundResult refundResult = new PaymentServiceClient.RefundResult(true, sampleBooking.getAmount(), null);
        when(paymentServiceClient.refund("bkg-12345", sampleBooking.getAmount(), "INR")).thenReturn(refundResult);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancellationResultDto result = bookingService.cancel("bkg-12345", new CancelBookingRequestDto("Plans changed"));

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.isRefundEligible()).isTrue();
        assertThat(result.getRefundStatus()).isEqualTo("INITIATED");
        assertThat(result.getRefundAmount()).isEqualTo(sampleBooking.getAmount());
        assertThat(sampleBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel marks NON_REFUNDABLE when hotel booking is within 48h of check-in")
    void cancel_hotelInside48h_nonRefundable() {
        sampleBooking.setStartDate(LocalDate.now()); // today, inside 48h
        when(bookingRepository.findById("bkg-12345")).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancellationResultDto result = bookingService.cancel("bkg-12345", new CancelBookingRequestDto("Last minute emergency"));

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.isRefundEligible()).isFalse();
        assertThat(result.getRefundStatus()).isEqualTo("NON_REFUNDABLE");
        verify(paymentServiceClient, never()).refund(any(), any(), any());
    }

    @Test
    @DisplayName("cancel throws IllegalStateException if booking is already CANCELLED")
    void cancel_alreadyCancelled_throwsIllegalStateException() {
        sampleBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById("bkg-12345")).thenReturn(Optional.of(sampleBooking));

        assertThatThrownBy(() -> bookingService.cancel("bkg-12345", new CancelBookingRequestDto("Duplicate")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel throws IllegalStateException if booking status is FAILED")
    void cancel_failedBooking_throwsIllegalStateException() {
        sampleBooking.setStatus(BookingStatus.FAILED);
        when(bookingRepository.findById("bkg-12345")).thenReturn(Optional.of(sampleBooking));

        assertThatThrownBy(() -> bookingService.cancel("bkg-12345", new CancelBookingRequestDto("Failed retry")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FAILED");

        verify(bookingRepository, never()).save(any());
    }
}
