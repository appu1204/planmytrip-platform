package com.planmytrip.trip_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAdvisoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String destination;
    private String status; // SAFE, CAUTION, DANGER
    private String title;
    private String professionalVerdict;
    private String recommendation;
    private int safetyScore;
    private double temperature;
    private double windSpeedKmh;
    private double precipitationMm;
    @com.fasterxml.jackson.annotation.JsonProperty("isHillyTerrain")
    private boolean isHillyTerrain;
    private List<String> hazardReasons;

    public boolean isHillyTerrain() {
        return isHillyTerrain;
    }
}
