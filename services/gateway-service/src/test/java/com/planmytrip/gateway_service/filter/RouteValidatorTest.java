package com.planmytrip.gateway_service.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private final RouteValidator validator = new RouteValidator();

    @Test
    void shouldRecognizeResendOtpAsPublicEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/user/auth/resend-otp")
                .build();

        assertFalse(validator.isSecured.test(request), "Resend OTP should be an open endpoint (not secured)");
    }

    @Test
    void shouldRecognizeProtectedEndpointsAsSecured() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/user/profile")
                .build();

        assertTrue(validator.isSecured.test(request), "Profile should be a secured endpoint");
    }
}
