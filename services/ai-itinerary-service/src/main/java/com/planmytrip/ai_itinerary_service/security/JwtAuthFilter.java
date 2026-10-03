package com.planmytrip.ai_itinerary_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Every feature in this service sits behind login (per product requirement:
 * "after login only they can access these features"). This filter:
 *   1. Reads the "Authorization: Bearer <token>" header
 *   2. Validates the signature using the shared secret
 *   3. Puts the resolved Long userId into the SecurityContext as the principal
 *
 * Controllers/services then read the current user via CurrentUserProvider,
 * never by trusting a userId sent in the request body.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Check for X-User-Id header forwarded by API Gateway
        String xUserId = request.getHeader("X-User-Id");
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                Long userId = Long.parseLong(xUserId.trim());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authentication.setDetails(request);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", xUserId);
            }
        }

        // 2. If not authenticated via X-User-Id, check Authorization: Bearer token (for direct service calls)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtUtil.isValid(token)) {
                    Long userId = jwtUtil.extractUserId(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    authentication.setDetails(request);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.debug("Rejected request to {} - invalid/expired token", request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
