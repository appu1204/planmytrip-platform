package com.planmytrip.booking_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Payload to create a hotel booking (Phase 3 — "Book a hotel").
 * POST /api/bookings/hotels
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelBookingRequestDto {

    @NotBlank(message = "userId is required")
    private String userId;

    private String tripId;

    @NotBlank(message = "hotelId is required")
    private String hotelId;

    @NotBlank(message = "roomTypeId is required")
    private String roomTypeId;

    @NotNull(message = "checkIn date is required")
    @FutureOrPresent(message = "checkIn cannot be in the past")
    private LocalDate checkIn;

    @NotNull(message = "checkOut date is required")
    private LocalDate checkOut;

    @NotNull(message = "adults count is required")
    @Min(value = 1, message = "at least 1 adult is required")
    @Builder.Default
    private Integer adults = 1;

    @Min(value = 0)
    @Builder.Default
    private Integer children = 0;

    public int getAdults() {
        return adults != null ? adults : 1;
    }

    public int getChildren() {
        return children != null ? children : 0;
    }

    @NotBlank(message = "primary guest name is required")
    private String guestFullName;

    @Email(message = "guestEmail must be a valid email")
    @NotBlank(message = "guestEmail is required")
    private String guestEmail;

    @NotBlank(message = "guestPhone is required")
    private String guestPhone;

    private String specialRequests;

    /** AssertTrue is evaluated by the bean validator on top of field-level checks. */
    @AssertTrue(message = "checkOut must be after checkIn")
    public boolean isDateRangeValid() {
        return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
    }
}

