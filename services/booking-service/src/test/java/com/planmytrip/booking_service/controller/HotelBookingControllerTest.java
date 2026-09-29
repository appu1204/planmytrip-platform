package com.planmytrip.booking_service.controller;

import com.planmytrip.booking_service.dto.response.HotelDetailDto;
import com.planmytrip.booking_service.dto.response.HotelSearchPageResponse;
import com.planmytrip.booking_service.dto.response.HotelSearchResultDto;
import com.planmytrip.booking_service.dto.response.RoomTypeDto;
import com.planmytrip.booking_service.exception.HotelNotFoundException;
import com.planmytrip.booking_service.service.HotelBookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotelBookingController.class)
class HotelBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HotelBookingService hotelBookingService;

    @Test
    @DisplayName("GET /api/bookings/hotels/search - 200 OK with valid parameters")
    void searchHotels_success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        HotelSearchResultDto sample = HotelSearchResultDto.builder()
                .hotelId("HTL123")
                .name("Grand Luxury Palace")
                .location("Delhi")
                .ratingScore(4.5)
                .reviewCount(120)
                .pricePerNight(new BigDecimal("3500.00"))
                .currency("INR")
                .amenities(List.of("WiFi", "Pool"))
                .freeCancellation(true)
                .build();

        HotelSearchPageResponse pageResponse = HotelSearchPageResponse.builder()
                .content(List.of(sample))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(hotelBookingService.search(eq("Delhi"), eq(checkIn), eq(checkOut), eq(2), eq(1), any(), any(), eq(0), eq(20)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/bookings/hotels/search")
                        .param("destination", "Delhi")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "2")
                        .param("children", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].hotelId").value("HTL123"))
                .andExpect(jsonPath("$.content[0].name").value("Grand Luxury Palace"))
                .andExpect(jsonPath("$.content[0].pricePerNight").value(3500.00));
    }

    @Test
    @DisplayName("GET /api/bookings/hotels/search - 400 Bad Request when destination is blank")
    void searchHotels_blankDestination() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        mockMvc.perform(get("/api/bookings/hotels/search")
                        .param("destination", "  ")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/bookings/hotels/search - 400 Bad Request when checkIn is missing")
    void searchHotels_missingCheckIn() throws Exception {
        mockMvc.perform(get("/api/bookings/hotels/search")
                        .param("destination", "Delhi"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/bookings/hotels/{hotelId} - 200 OK with valid parameters")
    void getDetail_success() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(6);

        RoomTypeDto room = RoomTypeDto.builder()
                .roomTypeId("RATE-9988")
                .name("Deluxe Room")
                .sleeps(2)
                .pricePerNight(new BigDecimal("4200.00"))
                .currency("INR")
                .available(true)
                .refundable(true)
                .freeCancellationUntil("2026-10-10")
                .build();

        HotelDetailDto detail = HotelDetailDto.builder()
                .hotelId("178092")
                .name("Shervani Nehru Place")
                .location("Delhi and NCR")
                .ratingScore(4.0)
                .reviewCount(50)
                .amenities(List.of("WiFi", "Restaurant", "Room service"))
                .roomTypes(List.of(room))
                .cancellationPolicy("Free cancellation up to 48 hours before check-in")
                .build();

        when(hotelBookingService.getDetail(eq("178092"), eq(checkIn), eq(checkOut)))
                .thenReturn(detail);

        mockMvc.perform(get("/api/bookings/hotels/178092")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value("178092"))
                .andExpect(jsonPath("$.name").value("Shervani Nehru Place"))
                .andExpect(jsonPath("$.roomTypes[0].roomTypeId").value("RATE-9988"))
                .andExpect(jsonPath("$.roomTypes[0].name").value("Deluxe Room"))
                .andExpect(jsonPath("$.roomTypes[0].pricePerNight").value(4200.00));
    }

    @Test
    @DisplayName("GET /api/bookings/hotels/{hotelId} - 404 Not Found when hotel does not exist")
    void getDetail_notFound() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(6);

        when(hotelBookingService.getDetail(eq("99999999"), eq(checkIn), eq(checkOut)))
                .thenThrow(new HotelNotFoundException("99999999"));

        mockMvc.perform(get("/api/bookings/hotels/99999999")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("HOTEL_NOT_FOUND"));
    }
}
