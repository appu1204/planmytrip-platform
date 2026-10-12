package com.planmytrip.booking_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Outcome of DELETE /api/bookings/{bookingId} (Phase 5 — cancel).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResultDto {
    private String bookingId;
    private String status; // CANCELLED
    private boolean refundEligible;
    private BigDecimal refundAmount;
    private String refundStatus; // e.g. "INITIATED", "NOT_APPLICABLE"
}
