package com.planmytrip.trip_service.dto.response;

import java.util.UUID;

import com.planmytrip.trip_service.enums.TripPreferenceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TripPreferencesResponse {

    private UUID tripId;
    private List<TripPreferenceType> preferences;

}
