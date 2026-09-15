package com.planmytrip.trip_service.config;

import com.planmytrip.trip_service.enums.TripStatus;
import com.planmytrip.trip_service.enums.TripType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, TripType>() {
            @Override
            public TripType convert(String source) {
                return TripType.fromString(source);
            }
        });

        registry.addConverter(new Converter<String, TripStatus>() {
            @Override
            public TripStatus convert(String source) {
                return TripStatus.fromString(source);
            }
        });
    }
}
