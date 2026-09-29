package com.planmytrip.booking_service.client;

import java.math.BigDecimal;

/**
 * REST client to the Payment Service, wrapped with a circuit breaker in
 * a real deployment (e.g. Resilience4j). Kept as a thin interface so
 * BookingService never depends on the payment provider directly.
 */
public interface PaymentServiceClient {

    ChargeResult charge(String bookingId, BigDecimal amount, String currency);

    RefundResult refund(String bookingId, BigDecimal amount, String currency);

    record ChargeResult(boolean success, String paymentId, String failureReason) {}

    record RefundResult(boolean success, BigDecimal refundedAmount, String failureReason) {}
}
