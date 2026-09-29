package com.planmytrip.booking_service.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.planmytrip.booking_service.dto.response.HotelSearchPageResponse;
import com.planmytrip.booking_service.service.HotelBookingService;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/bookings/hotels")
@RequiredArgsConstructor 
@Validated
public class HotelBookingController {

    private final HotelBookingService hotelBookingService;

    /**
     * GET /api/bookings/hotels/search
     * This endpoint searches available hotels for a destination and date range with pagination.
     *
     * @param destination City or destination name (cannot be blank)
     * @param checkIn     Check-in date (ISO-8601 YYYY-MM-DD)
     * @param checkOut    Check-out date (ISO-8601 YYYY-MM-DD)
     * @param adults      Number of adult guests (min 1, default 1)
     * @param children    Number of child guests (min 0, default 0)
     * @param minPrice    Optional minimum price filter
     * @param maxPrice    Optional maximum price filter
     * @param page        Page index (0-based, default 0)
     * @param size        Page size (min 1, default 20)
     * @return Paginated search response with matching hotels and page metadata
     */
    @GetMapping("/search")
    public ResponseEntity<HotelSearchPageResponse> search(
            @RequestParam @NotBlank(message = "Destination cannot be blank") String destination,
            @RequestParam @NotNull(message = "checkIn date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @NotNull(message = "checkOut date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Adults must be at least 1") int adults,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Children count cannot be negative") int children,
            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "minPrice cannot be negative") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.0", message = "maxPrice cannot be negative") BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index cannot be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") int size) {

        HotelSearchPageResponse response = hotelBookingService.search(
                destination, checkIn, checkOut, adults, children, minPrice, maxPrice, page, size);
        return ResponseEntity.ok(response);
    }

        
}
