package com.planmytrip.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Binds hotelbeds.* properties from application-dev.properties
 * (or an env-var override) into a typed config bean.
 *
 * Required properties:
 *   hotelbeds.api-key=<your api key from developer.hotelbeds.com>
 *   hotelbeds.secret=<your secret from the same key detail page>
 *
 * Optional (defaults point at the free test/evaluation environment):
 *   hotelbeds.content-base-url=https://api.test.hotelbeds.com/hotel-content-api/1.0
 *   hotelbeds.booking-base-url=https://api.test.hotelbeds.com/hotel-api/1.0
 */

@Configuration 
@ConfigurationProperties (prefix = "hotelbeds")
@Data 

public class HotelbedsProperties {

    private String apiKey;
    private String secret;

    private String contentBaseUrl = "https://api.test.hotelbeds.com/hotel-content-api/1.0";
    private String bookingBaseUrl = "https://api.test.hotelbeds.com/hotel-api/1.0";

}
