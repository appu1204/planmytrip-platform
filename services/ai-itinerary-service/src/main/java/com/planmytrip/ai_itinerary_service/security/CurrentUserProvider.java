package com.planmytrip.ai_itinerary_service.security;


import com.planmytrip.ai_itinerary_service.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new UnauthorizedException("No authenticated user found on request");
        }
        return (Long) auth.getPrincipal();
    }
}
