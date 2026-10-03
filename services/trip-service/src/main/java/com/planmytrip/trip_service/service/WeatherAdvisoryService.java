package com.planmytrip.trip_service.service;

import com.planmytrip.trip_service.dto.response.WeatherAdvisoryResponse;

public interface WeatherAdvisoryService {
    WeatherAdvisoryResponse getAdvisory(String destination);
}
