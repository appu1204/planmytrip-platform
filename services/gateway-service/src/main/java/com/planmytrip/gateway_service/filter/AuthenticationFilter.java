package com.planmytrip.gateway_service.filter;

import com.planmytrip.gateway_service.config.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Runs on every request. Public endpoints (see RouteValidator) pass
 * straight through. Everything else must present a valid
 * "Authorization: Bearer <token>" header. On success we strip the
 * Authorization header and instead forward the resolved user id as
 * X-User-Id, so downstream services never have to parse JWTs themselves.
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator routeValidator, JwtUtil jwtUtil) {
        this.routeValidator = routeValidator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!routeValidator.isSecured.test(request)) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Claims claims = jwtUtil.validateAndExtractClaims(token);
                    String userId = jwtUtil.extractUserId(claims);
                    if (userId != null) {
                        ServerHttpRequest authenticated = request.mutate()
                                .headers(headers -> headers.set("X-User-Id", userId))
                                .build();
                        return chain.filter(exchange.mutate().request(authenticated).build());
                    }
                } catch (Exception ignored) {
                    // Fall back to sanitized unauthenticated request
                }
            }

            // Strip any external untrusted X-User-Id on open endpoints to prevent header spoofing
            ServerHttpRequest sanitized = request.mutate()
                    .headers(headers -> headers.remove("X-User-Id"))
                    .build();
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtractClaims(token);
            String userId = jwtUtil.extractUserId(claims);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.set("X-User-Id", userId);
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException e) {
            return unauthorized(exchange, e.getMessage());
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // run before routing
    }
}
