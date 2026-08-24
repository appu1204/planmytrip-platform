package com.planmytrip.gateway_service.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Decides whether an incoming request needs a valid JWT before it's
 * forwarded downstream. Add new public (no-auth) paths here as new
 * services and endpoints come online.
 */
@Component
public class RouteValidator {

    // Exact-prefix match is enough here since none of these paths share
    // a prefix with a protected one.
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/user/auth/register",
            "/api/user/auth/login",
            "/api/user/auth/verify-otp",
            "/api/user/auth/forgot-password",
            "/api/user/auth/reset-password",
            "/api/user/auth/verify-email",

            // Public "browse without login" endpoints
            "/api/trip/popular",

            // Gateway/actuator health checks
            "/actuator/health"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_API_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().startsWith(uri));
}
