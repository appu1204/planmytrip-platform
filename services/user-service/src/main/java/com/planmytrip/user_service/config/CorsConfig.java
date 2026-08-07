

package com.planmytrip.user_service.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow your frontend origin
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // Allow all standard HTTP methods + OPTIONS for preflight
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers (including Authorization for JWT)
        config.setAllowedHeaders(List.of("*"));

        // Allow cookies / credentials
        config.setAllowCredentials(true);

        // Expose Authorization header to the frontend if needed
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS to ALL paths (not just /api/**)
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}