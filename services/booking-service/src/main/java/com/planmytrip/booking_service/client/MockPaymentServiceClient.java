package com.planmytrip.booking_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock / default implementation of PaymentServiceClient for local development
 * and testing. When a dedicated payment-service is deployed, this bean can be
 * superseded by a Feign client or REST client.
 */
@Component
@Profile({"dev", "test", "local"})
@ConditionalOnMissingBean(name = "paymentServiceFeignClient")
@Slf4j
public class MockPaymentServiceClient implements PaymentServiceClient {

    @Override
    public ChargeResult charge(String bookingId, BigDecimal amount, String currency) {
        log.info("Processing simulated payment charge: bookingId={}, amount={} {}", bookingId, amount, currency);
        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new ChargeResult(true, paymentId, null);
    }

    @Override
    public RefundResult refund(String bookingId, BigDecimal amount, String currency) {
        log.info("Processing simulated payment refund: bookingId={}, amount={} {}", bookingId, amount, currency);
        return new RefundResult(true, amount, null);
    }
}
