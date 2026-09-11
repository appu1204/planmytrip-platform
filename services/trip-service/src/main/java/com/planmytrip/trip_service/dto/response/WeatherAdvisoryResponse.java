package com.planmytrip.trip_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAdvisoryResponse {
    private String destination;
    private String status; // SAFE, CAUTION, DANGER
    private String title;
    private String professionalVerdict;
    private String recommendation;
    private int safetyScore;
    private double temperature;
    private double windSpeedKmh;
    private double precipitationMm;
    private boolean isHillyTerrain;
    private List<String> hazardReasons;
}
