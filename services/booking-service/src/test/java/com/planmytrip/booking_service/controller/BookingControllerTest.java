package com.planmytrip.booking_service.controller;

import com.planmytrip.booking_service.dto.request.CancelBookingRequestDto;
import com.planmytrip.booking_service.dto.response.BookingResponseDto;
import com.planmytrip.booking_service.dto.response.CancellationResultDto;
import com.planmytrip.booking_service.enums.BookingModule;
import com.planmytrip.booking_service.enums.BookingStatus;
import com.planmytrip.booking_service.exception.BookingNotFoundException;
import com.planmytrip.booking_service.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    // =========================================================================
    // 1. GET /api/bookings/{bookingId} Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/bookings/{bookingId} - 200 OK with valid hotel booking confirmation")
    void getBooking_hotelBooking_success() throws Exception {
        String bookingId = "bkg-hotel-12345";
        BookingResponseDto responseDto = BookingResponseDto.builder()
                .bookingId(bookingId)
                .module(BookingModule.HOTEL)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-HTL-8823041")
                .itemName("Kumarakom Lake Resort")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 5))
                .amount(new BigDecimal("25000.00"))
                .currency("INR")
                .build();

        when(bookingService.getById(bookingId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.module").value("HOTEL"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.referenceNo").value("PMT-HTL-8823041"))
                .andExpect(jsonPath("$.itemName").value("Kumarakom Lake Resort"))
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.endDate").value("2026-11-05"))
                .andExpect(jsonPath("$.amount").value(25000.00))
                .andExpect(jsonPath("$.currency").value("INR"));

        verify(bookingService).getById(bookingId);
    }

    @Test
    @DisplayName("GET /api/bookings/{bookingId} - 200 OK verifies identical response shape across Flight/Transport/Activity")
    void getBooking_flightBooking_sameShape_success() throws Exception {
        String bookingId = "bkg-flt-98765";
        BookingResponseDto responseDto = BookingResponseDto.builder()
                .bookingId(bookingId)
                .module(BookingModule.FLIGHT)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-FLT-1029384")
                .itemName("Air India AI-101 DEL -> BLR")
                .startDate(LocalDate.of(2026, 12, 10))
                .endDate(LocalDate.of(2026, 12, 10))
                .amount(new BigDecimal("7500.00"))
                .currency("INR")
                .build();

        when(bookingService.getById(bookingId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.module").value("FLIGHT"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.referenceNo").value("PMT-FLT-1029384"))
                .andExpect(jsonPath("$.itemName").value("Air India AI-101 DEL -> BLR"))
                .andExpect(jsonPath("$.startDate").value("2026-12-10"))
                .andExpect(jsonPath("$.endDate").value("2026-12-10"))
                .andExpect(jsonPath("$.amount").value(7500.00))
                .andExpect(jsonPath("$.currency").value("INR"));

        verify(bookingService).getById(bookingId);
    }

    @Test
    @DisplayName("GET /api/bookings/{bookingId} - 404 NOT_FOUND when booking does not exist")
    void getBooking_notFound_returns404() throws Exception {
        String nonExistentId = "bkg-unknown-999";
        when(bookingService.getById(nonExistentId)).thenThrow(new BookingNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/bookings/{bookingId}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Booking not found: " + nonExistentId));

        verify(bookingService).getById(nonExistentId);
    }

    @Test
    @DisplayName("GET /api/bookings/{bookingId} - 400 BAD_REQUEST when bookingId is blank/whitespace")
    void getBooking_blankId_returns400() throws Exception {
        mockMvc.perform(get("/api/bookings/{bookingId}", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Booking ID cannot be blank"));
    }

    // =========================================================================
    // 2. GET /api/bookings (listBookings) Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/bookings - 200 OK returns list of bookings for a user")
    void listBookings_success() throws Exception {
        String userId = "usr-123";
        BookingResponseDto item1 = BookingResponseDto.builder()
                .bookingId("bkg-001")
                .module(BookingModule.HOTEL)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-HTL-001")
                .itemName("Taj Palace")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 3))
                .amount(new BigDecimal("12000.00"))
                .currency("INR")
                .build();

        BookingResponseDto item2 = BookingResponseDto.builder()
                .bookingId("bkg-002")
                .module(BookingModule.FLIGHT)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-FLT-002")
                .itemName("IndiGo 6E-205")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 1))
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .build();

        when(bookingService.list(userId, null, null)).thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/api/bookings")
                        .param("userId", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].bookingId").value("bkg-001"))
                .andExpect(jsonPath("$[0].module").value("HOTEL"))
                .andExpect(jsonPath("$[1].bookingId").value("bkg-002"))
                .andExpect(jsonPath("$[1].module").value("FLIGHT"));

        verify(bookingService).list(userId, null, null);
    }

    @Test
    @DisplayName("GET /api/bookings - 200 OK returns filtered bookings by module and tripId")
    void listBookings_withModuleAndTripFilter_success() throws Exception {
        String userId = "usr-123";
        String tripId = "trip-456";
        BookingResponseDto item = BookingResponseDto.builder()
                .bookingId("bkg-001")
                .module(BookingModule.HOTEL)
                .status(BookingStatus.CONFIRMED)
                .referenceNo("PMT-HTL-001")
                .itemName("Taj Palace")
                .amount(new BigDecimal("12000.00"))
                .currency("INR")
                .build();

        when(bookingService.list(userId, BookingModule.HOTEL, tripId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/bookings")
                        .param("userId", userId)
                        .param("module", "HOTEL")
                        .param("tripId", tripId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingId").value("bkg-001"))
                .andExpect(jsonPath("$[0].module").value("HOTEL"));

        verify(bookingService).list(userId, BookingModule.HOTEL, tripId);
    }

    @Test
    @DisplayName("GET /api/bookings - 200 OK returns empty array when user has no bookings")
    void listBookings_emptyList_success() throws Exception {
        String userId = "usr-999";
        when(bookingService.list(userId, null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/bookings")
                        .param("userId", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService).list(userId, null, null);
    }

    @Test
    @DisplayName("GET /api/bookings - 400 BAD_REQUEST when userId is missing")
    void listBookings_missingUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
    }

    @Test
    @DisplayName("GET /api/bookings - 400 BAD_REQUEST when userId is blank")
    void listBookings_blankUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/bookings")
                        .param("userId", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // =========================================================================
    // 3. DELETE /api/bookings/{bookingId} (cancelBooking) Tests
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/bookings/{bookingId} - 200 OK cancels booking with refund initiated")
    void cancelBooking_success_withRefund() throws Exception {
        String bookingId = "bkg-001";
        CancellationResultDto result = CancellationResultDto.builder()
                .bookingId(bookingId)
                .status("CANCELLED")
                .refundEligible(true)
                .refundAmount(new BigDecimal("12000.00"))
                .refundStatus("INITIATED")
                .build();

        when(bookingService.cancel(eq(bookingId), any(CancelBookingRequestDto.class))).thenReturn(result);

        String requestBody = "{\"reason\": \"Change of travel plans\"}";

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundEligible").value(true))
                .andExpect(jsonPath("$.refundAmount").value(12000.00))
                .andExpect(jsonPath("$.refundStatus").value("INITIATED"));

        verify(bookingService).cancel(eq(bookingId), any(CancelBookingRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/bookings/{bookingId} - 404 NOT_FOUND when booking does not exist")
    void cancelBooking_notFound_returns404() throws Exception {
        String nonExistentId = "bkg-unknown-999";
        when(bookingService.cancel(eq(nonExistentId), any(CancelBookingRequestDto.class)))
                .thenThrow(new BookingNotFoundException(nonExistentId));

        String requestBody = "{\"reason\": \"Flight cancelled\"}";

        mockMvc.perform(delete("/api/bookings/{bookingId}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Booking not found: " + nonExistentId));
    }

    @Test
    @DisplayName("DELETE /api/bookings/{bookingId} - 409 CONFLICT when booking is already cancelled")
    void cancelBooking_alreadyCancelled_returns409() throws Exception {
        String bookingId = "bkg-already-cancelled";
        when(bookingService.cancel(eq(bookingId), any(CancelBookingRequestDto.class)))
                .thenThrow(new IllegalStateException("Booking " + bookingId + " is already cancelled"));

        String requestBody = "{\"reason\": \"Duplicate cancellation attempt\"}";

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE"))
                .andExpect(jsonPath("$.message").value("Booking " + bookingId + " is already cancelled"));
    }

    @Test
    @DisplayName("DELETE /api/bookings/{bookingId} - 400 BAD_REQUEST when cancellation reason is blank")
    void cancelBooking_blankReason_returns400() throws Exception {
        String bookingId = "bkg-001";
        String requestBody = "{\"reason\": \"   \"}";

        mockMvc.perform(delete("/api/bookings/{bookingId}", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("cancellation reason is required"));
    }
}
