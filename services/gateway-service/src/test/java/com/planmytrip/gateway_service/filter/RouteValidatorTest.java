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
    void shouldRecognizePopularDestinationsAsPublicEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/trip/popular")
                .build();

        assertFalse(validator.isSecured.test(request), "Popular destinations should be open without auth");
    }

    @Test
    void shouldRecognizeActuatorHealthAsPublicEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();

        assertFalse(validator.isSecured.test(request), "Actuator health should be open for load balancers");
    }

    @Test
    void shouldRecognizeTripModificationAsSecuredEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .patch("/api/trip/trips/123e4567-e89b-12d3-a456-426614174000")
                .build();

        assertTrue(validator.isSecured.test(request), "Trip details and mutations must be secured");
    }

    @Test
    void shouldRecognizeItineraryGenerationAsSecuredEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/api/v1/ai-itinerary/generate")
                .build();

        assertTrue(validator.isSecured.test(request), "AI itinerary generation must be secured");
    }
}
