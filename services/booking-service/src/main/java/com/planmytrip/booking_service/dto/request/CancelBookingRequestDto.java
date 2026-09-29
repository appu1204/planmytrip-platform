package com.planmytrip.booking_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DELETE /api/bookings/{bookingId} body — cancellation reason + implicit booking id (path var).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequestDto {

    @NotBlank(message = "cancellation reason is required")
    private String reason;
}